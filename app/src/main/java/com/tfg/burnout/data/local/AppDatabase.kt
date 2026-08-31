package com.tfg.burnout.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tfg.burnout.data.local.security.GestorClaveBd
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.tfg.burnout.data.local.dao.BiometriaDao
import com.tfg.burnout.data.local.dao.CesqtDao
import com.tfg.burnout.data.local.dao.MetaDao
import com.tfg.burnout.data.local.dao.RecomendacionDao
import com.tfg.burnout.data.local.dao.SedeCopDao
import com.tfg.burnout.data.local.dao.UsuarioDao
import com.tfg.burnout.data.local.entity.BiometriaEntity
import com.tfg.burnout.data.local.entity.CesqtResponseEntity
import com.tfg.burnout.data.local.entity.MetaEntity
import com.tfg.burnout.data.local.entity.RecomendacionEntity
import com.tfg.burnout.data.local.entity.SedeCopEntity
import com.tfg.burnout.data.local.entity.UsuarioEntity
import com.tfg.burnout.data.local.seed.SedesCopSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base de datos local de la aplicación (Room sobre SQLite).
 * Persistencia REAL: los datos sobreviven a reinicios; no es caché (§4.4).
 *
 * CIFRADO EN REPOSO (§7.3) — IMPLEMENTADO.
 * Room y SQLite no cifran nada por defecto, y esta base alberga datos de
 * salud, categoría especial del artículo 9 del RGPD. Por ello se cifra con
 * SQLCipher (AES-256) mediante una SupportFactory, con una passphrase
 * generada aleatoriamente en el primer arranque y custodiada por el Android
 * Keystore (ver data/local/security/GestorClaveBd). Aunque se extraiga el
 * fichero de la base de datos del dispositivo, su contenido resulta
 * ilegible sin esa clave, que no reside en claro en ningún punto. El cifrado
 * de disco del propio sistema operativo actúa como segunda capa.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        CesqtResponseEntity::class,
        BiometriaEntity::class,
        SedeCopEntity::class,
        MetaEntity::class,
        RecomendacionEntity::class
    ],
    version = 3,  // v2: perfil de contexto laboral en UsuarioEntity (§5.5)
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun cesqtDao(): CesqtDao
    abstract fun biometriaDao(): BiometriaDao
    abstract fun sedeCopDao(): SedeCopDao
    abstract fun metaDao(): MetaDao
    abstract fun recomendacionDao(): RecomendacionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun obtener(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val app = context.applicationContext

                // Carga de las bibliotecas nativas de SQLCipher antes de
                // abrir la base: sin esto la factoría no puede operar.
                SQLiteDatabase.loadLibs(app)

                // APERTURA TOLERANTE A FALLOS.
                //
                // Room no abre el fichero al construir el objeto, sino en el
                // primer acceso, de modo que un error de apertura afloraría
                // más tarde y derribaría la aplicación en un punto arbitrario.
                // Aquí se fuerza la apertura de inmediato y se captura el
                // fallo. El caso realista es la transición de una instalación
                // previa SIN cifrar a esta versión cifrada: SQLCipher no puede
                // leer ese fichero y lo rechaza. Al tratarse de un prototipo
                // sin datos de producción que preservar, se descarta la base
                // anterior y se recrea cifrada, en lugar de exigir al usuario
                // que desinstale la aplicación.
                val db = runCatching { construir(app).apreturaForzada() }
                    .getOrElse {
                        app.deleteDatabase(NOMBRE_BD)
                        construir(app).apreturaForzada()
                    }
                INSTANCE = db

                // Asset seeding: precargamos las sedes del COP la primera vez.
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        if (db.sedeCopDao().contar() == 0) {
                            db.sedeCopDao().insertarTodas(SedesCopSeed.sedes)
                        }
                        // Aseguramos un usuario por defecto.
                        if (db.usuarioDao().obtener() == null) {
                            db.usuarioDao().guardar(UsuarioEntity())
                        }
                    }
                }
                db
            }
        }

        private const val NOMBRE_BD = "burnout.db"

        private fun construir(app: Context): AppDatabase =
            Room.databaseBuilder(app, AppDatabase::class.java, NOMBRE_BD)
                // CIFRADO AES-256 con la clave custodiada por el Keystore.
                .openHelperFactory(SupportFactory(GestorClaveBd.passphrase(app)))
                // Prototipo académico: ante un cambio de esquema se recrea la
                // base (no hay datos de producción que preservar).
                .fallbackToDestructiveMigration()
                .build()

        /** Fuerza la apertura real del fichero para detectar fallos aquí. */
        private fun AppDatabase.apreturaForzada(): AppDatabase =
            also { it.openHelper.writableDatabase }
    }
}
