package com.example.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------------------------------------------------
// VIEWMODEL: contiene la lógica de negocio de la pantalla Home.
//
// - Sobrevive a los cambios de configuración (rotaciones).
// - Expone el estado a la UI mediante StateFlow.
// - Recibe acciones de la UI (insertar, borrar, etc.).
// - Habla con el repositorio, no directamente con Room/DataStore.
// ---------------------------------------------------------
class VideojuegoViewModel(
    private val repository: VideojuegoRepository
) : ViewModel() {

    // ---------------------------------------------------------
    // ESTADO: lista de videojuegos como StateFlow.
    //
    // stateIn convierte el Flow del repositorio en un StateFlow:
    //   - viewModelScope: se cancela cuando el ViewModel se destruye.
    //   - SharingStarted.WhileSubscribed(5000): mantiene el Flow activo
    //     mantiene suscriptores durante 5s tras girar pantalla.
    //   - emptyList(): valor inicial mientras llega el primer dato.
    // ---------------------------------------------------------
    val videojuegos: StateFlow<List<Videojuego>> = repository.videojuegos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ---------------------------------------------------------
    // ESTADO: email del usuario como StateFlow.
    // ---------------------------------------------------------
    val emailUsuario: StateFlow<String> = repository.emailUsuario
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    // ---------------------------------------------------------
    // ACCIONES: la UI llama a estas funciones.
    // Cada una lanza una corrutina en viewModelScope.
    // ---------------------------------------------------------

    fun insertar(videojuego: Videojuego) {
        viewModelScope.launch {
            repository.insertar(videojuego)
        }
    }

    fun actualizar(videojuego: Videojuego) {
        viewModelScope.launch {
            repository.actualizar(videojuego)
        }
    }

    fun borrar(videojuego: Videojuego) {
        viewModelScope.launch {
            repository.borrar(videojuego)
        }
    }

    fun borrarTodo() {
        viewModelScope.launch {
            repository.borrarTodo()
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            repository.cerrarSesion()
        }
    }
}

// ---------------------------------------------------------
// FACTORY: le dice a Compose cómo construir el ViewModel.
//
// El ViewModel necesita un repositorio, que no puede crear solo.
// La factory se lo pasa.
// ---------------------------------------------------------
class VideojuegoViewModelFactory(
    private val repository: VideojuegoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideojuegoViewModel::class.java)) {
            return VideojuegoViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
