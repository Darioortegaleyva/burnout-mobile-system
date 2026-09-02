package com.tfg.burnout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tfg.burnout.data.local.entity.BiometriaEntity
import com.tfg.burnout.data.local.entity.CesqtResponseEntity
import com.tfg.burnout.data.local.entity.MetaEntity
import com.tfg.burnout.data.local.entity.RecomendacionEntity
import com.tfg.burnout.data.local.entity.SedeCopEntity
import com.tfg.burnout.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario WHERE id = 1")
    fun observar(): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuario WHERE id = 1")
    suspend fun obtener(): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: UsuarioEntity)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)
}

@Dao
interface CesqtDao {
    @Query("DELETE FROM cesqt_response")
    suspend fun borrarTodo()

    @Insert
    suspend fun insertar(respuesta: CesqtResponseEntity): Long

    /**
     * La ordenación desempata por id, igual que [penultimo]. Sin ese
     * desempate, dos evaluaciones del MISMO día quedaban empatadas y SQLite
     * devolvía la primera insertada: al reevaluar en el día, el índice se
     * seguía calculando con las respuestas viejas y [resumenDelCiclo]
     * comparaba una fila consigo misma.
     */
    @Query("SELECT * FROM cesqt_response ORDER BY fechaEpochDay DESC, id DESC LIMIT 1")
    suspend fun ultimo(): CesqtResponseEntity?

    /** Evaluación anterior a la última, para comparar ciclos (CU-04). */
    @Query("SELECT * FROM cesqt_response ORDER BY fechaEpochDay DESC, id DESC LIMIT 1 OFFSET 1")
    suspend fun penultimo(): CesqtResponseEntity?

    @Query("SELECT * FROM cesqt_response ORDER BY fechaEpochDay ASC")
    fun observarHistorico(): Flow<List<CesqtResponseEntity>>
}

@Dao
interface BiometriaDao {
    @Query("DELETE FROM biometria")
    suspend fun borrarTodo()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(registro: BiometriaEntity)

    @Query("SELECT * FROM biometria ORDER BY fechaEpochDay DESC LIMIT 1")
    suspend fun masReciente(): BiometriaEntity?

    /** Últimos N días para computar la media móvil de la línea base. */
    @Query("SELECT * FROM biometria ORDER BY fechaEpochDay DESC LIMIT :dias")
    suspend fun ultimos(dias: Int): List<BiometriaEntity>
}

@Dao
interface SedeCopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(sedes: List<SedeCopEntity>)

    @Query("SELECT COUNT(*) FROM sede_cop")
    suspend fun contar(): Int

    @Query("SELECT * FROM sede_cop WHERE provincia = :provincia LIMIT 1")
    suspend fun buscarPorProvincia(provincia: String): SedeCopEntity?

    @Query("SELECT provincia FROM sede_cop ORDER BY provincia ASC")
    suspend fun listarProvincias(): List<String>
}

@Dao
interface MetaDao {
    @Query("DELETE FROM meta")
    suspend fun borrarTodo()

    @Insert
    suspend fun insertar(meta: MetaEntity): Long

    @Update
    suspend fun actualizar(meta: MetaEntity)

    @Query("SELECT * FROM meta WHERE activa = 1 ORDER BY fechaCreacionEpochDay DESC")
    fun observarActivas(): Flow<List<MetaEntity>>

    /** Lectura puntual de los retos activos (para el cierre de ciclo). */
    @Query("SELECT * FROM meta WHERE activa = 1")
    suspend fun activas(): List<MetaEntity>
}

@Dao
interface RecomendacionDao {
    @Query("DELETE FROM recomendacion")
    suspend fun borrarTodo()

    @Insert
    suspend fun insertar(recomendacion: RecomendacionEntity): Long

    @Update
    suspend fun actualizar(recomendacion: RecomendacionEntity)

    /** Pautas recomendadas desde una fecha dada (para no repetirlas). */
    @Query("SELECT * FROM recomendacion WHERE fechaEpochDay >= :desdeEpochDay")
    suspend fun desde(desdeEpochDay: Long): List<RecomendacionEntity>

    /** Última pauta recomendada, para preguntar qué tal fue. */
    @Query("SELECT * FROM recomendacion ORDER BY fechaEpochDay DESC, id DESC LIMIT 1")
    suspend fun ultima(): RecomendacionEntity?

    @Query("SELECT * FROM recomendacion ORDER BY fechaEpochDay DESC")
    suspend fun todas(): List<RecomendacionEntity>
}
