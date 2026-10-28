package com.example.login

import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------
// REPOSITORIO: capa intermedia entre la UI y las fuentes de datos.
//
// Une Room (videojuegos) y DataStore (sesión) en una sola API.
// La UI solo conoce esta clase; no sabe de Room ni de DataStore.
// ---------------------------------------------------------
class VideojuegoRepository(
    private val dao: VideojuegoDao,
    private val session: SessionDataStore
) {

    // ---------------------------------------------------------
    // VIDEOJUEGOS (vienen de Room)
    // ---------------------------------------------------------

    // Consulta reactiva: la UI se actualiza sola cuando cambia la tabla.
    val videojuegos: Flow<List<Videojuego>> = dao.getAll()

    // ---------------------------------------------------------
    // Operaciones de escritura.
    // Son `suspend` porque Room las ejecuta fuera del hilo principal.
    // ---------------------------------------------------------
    suspend fun insertar(videojuego: Videojuego) = dao.insert(videojuego)

    suspend fun actualizar(videojuego: Videojuego) = dao.update(videojuego)

    suspend fun borrar(videojuego: Videojuego) = dao.delete(videojuego)

    suspend fun borrarTodo() = dao.deleteAll()

    // ---------------------------------------------------------
    // SESIÓN (viene de DataStore)
    // ---------------------------------------------------------

    val emailUsuario: Flow<String> = session.emailFlow

    suspend fun guardarSesion(email: String, remember: Boolean) =
        session.saveSession(email, remember)

    suspend fun cerrarSesion() = session.clearSession()
}
