package com.example.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VideojuegoViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // ---------------------------------------------------------
    // PERMISO POST_NOTIFICATIONS (Android 13+)
    // ---------------------------------------------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ---------------------------------------------------------
    // ESTADO LOCAL DE LA UI
    // ---------------------------------------------------------
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Estado del diálogo de creación/edición
    var showFormDialog by remember { mutableStateOf(false) }
    var videojuegoEditando by remember { mutableStateOf<Videojuego?>(null) }

    // Estado del diálogo de confirmación de borrado
    var videojuegoABorrar by remember { mutableStateOf<Videojuego?>(null) }

    // ---------------------------------------------------------
    // ESTADO DEL VIEWMODEL
    // ---------------------------------------------------------
    val userEmail by viewModel.emailUsuario.collectAsState()
    val videojuegos by viewModel.videojuegos.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.abrir_menu)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_acerca_de)) },
                            onClick = {
                                menuExpanded = false
                                showAboutDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_cerrar_sesion)) },
                            onClick = {
                                menuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                videojuegoEditando = null
                showFormDialog = true
            }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.boton_add_videojuego)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Cabecera con el email ---
            Text(
                text = if (userEmail.isNotEmpty()) {
                    stringResource(R.string.home_bienvenida, userEmail)
                } else {
                    stringResource(R.string.home_sin_email)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // --- Título de la lista ---
            Text(
                text = stringResource(R.string.home_titulo_lista),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // --- Lista o mensaje vacío ---
            if (videojuegos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_lista_vacia),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(videojuegos, key = { it.id }) { videojuego ->
                        VideojuegoItem(
                            videojuego = videojuego,
                            onClick = {
                                videojuegoEditando = videojuego
                                showFormDialog = true
                            },
                            onDeleteClick = {
                                videojuegoABorrar = videojuego
                            },
                            onToggleCompletado = {
                                viewModel.actualizar(
                                    videojuego.copy(completado = !videojuego.completado)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // DIÁLOGO "Acerca de"
    // ---------------------------------------------------------
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.dialog_acerca_titulo)) },
            text = { Text(stringResource(R.string.dialog_acerca_texto)) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.dialog_aceptar))
                }
            }
        )
    }

    // ---------------------------------------------------------
    // DIÁLOGO CREAR / EDITAR
    // ---------------------------------------------------------
    val notifTitulo = stringResource(R.string.notif_titulo)
    val notifMensajeFormat = stringResource(R.string.notif_mensaje)

    if (showFormDialog) {
        VideojuegoFormDialog(
            videojuegoInicial = videojuegoEditando,
            onDismiss = {
                showFormDialog = false
                videojuegoEditando = null
            },
            onSave = { videojuego ->
                if (videojuegoEditando == null) {
                    // Es un videojuego nuevo: insertamos y notificamos.
                    viewModel.insertar(videojuego)

                    // Solo notificamos si el permiso está concedido.
                    val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true // En versiones anteriores, no hace falta permiso.
                    }

                    if (granted) {
                        NotificationHelper.mostrarNotificacion(
                            context = context,
                            titulo = notifTitulo,
                            mensaje = String.format(notifMensajeFormat, videojuego.titulo)
                        )
                    }
                } else {
                    // Es una edición: actualizamos sin notificar.
                    viewModel.actualizar(videojuego)
                }
                showFormDialog = false
                videojuegoEditando = null
            }
        )
    }

    // ---------------------------------------------------------
    // DIÁLOGO CONFIRMAR BORRADO
    // ---------------------------------------------------------
    videojuegoABorrar?.let { juego ->
        AlertDialog(
            onDismissRequest = { videojuegoABorrar = null },
            title = { Text(stringResource(R.string.dialog_borrar_titulo)) },
            text = {
                Text(stringResource(R.string.dialog_borrar_texto, juego.titulo))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.borrar(juego)
                    videojuegoABorrar = null
                }) {
                    Text(stringResource(R.string.boton_borrar))
                }
            },
            dismissButton = {
                TextButton(onClick = { videojuegoABorrar = null }) {
                    Text(stringResource(R.string.boton_cancelar))
                }
            }
        )
    }
}

// ---------------------------------------------------------
// ITEM DE LA LISTA: una fila por videojuego.
// ---------------------------------------------------------
@Composable
private fun VideojuegoItem(
    videojuego: Videojuego,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleCompletado: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = videojuego.completado,
                onCheckedChange = { onToggleCompletado() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = videojuego.titulo,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${videojuego.plataforma} · ${videojuego.genero} · ${videojuego.anyo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Nota: ${videojuego.nota}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_borrar_juego),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ---------------------------------------------------------
// DIÁLOGO DE FORMULARIO (crear o editar).
// ---------------------------------------------------------
@Composable
private fun VideojuegoFormDialog(
    videojuegoInicial: Videojuego?,
    onDismiss: () -> Unit,
    onSave: (Videojuego) -> Unit
) {
    val esEdicion = videojuegoInicial != null

    var titulo by remember { mutableStateOf(videojuegoInicial?.titulo ?: "") }
    var plataforma by remember { mutableStateOf(videojuegoInicial?.plataforma ?: "") }
    var genero by remember { mutableStateOf(videojuegoInicial?.genero ?: "") }
    var anyo by remember { mutableStateOf(videojuegoInicial?.anyo?.toString() ?: "") }
    var nota by remember { mutableStateOf(videojuegoInicial?.nota?.toString() ?: "") }
    var completado by remember { mutableStateOf(videojuegoInicial?.completado ?: false) }

    var tituloError by remember { mutableStateOf<String?>(null) }
    var anyoError by remember { mutableStateOf<String?>(null) }
    var notaError by remember { mutableStateOf<String?>(null) }

    val errorTituloText = stringResource(R.string.error_titulo_vacio)
    val errorAnyoText = stringResource(R.string.error_anyo_invalido)
    val errorNotaText = stringResource(R.string.error_nota_invalida)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (esEdicion) R.string.dialog_editar_titulo
                    else R.string.dialog_nuevo_titulo
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        tituloError = null
                    },
                    label = { Text(stringResource(R.string.hint_titulo_juego)) },
                    isError = tituloError != null,
                    supportingText = tituloError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = plataforma,
                    onValueChange = { plataforma = it },
                    label = { Text(stringResource(R.string.hint_plataforma)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = genero,
                    onValueChange = { genero = it },
                    label = { Text(stringResource(R.string.hint_genero)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = anyo,
                    onValueChange = {
                        anyo = it.filter { c -> c.isDigit() }
                        anyoError = null
                    },
                    label = { Text(stringResource(R.string.hint_anyo)) },
                    isError = anyoError != null,
                    supportingText = anyoError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nota,
                    onValueChange = {
                        nota = it
                        notaError = null
                    },
                    label = { Text(stringResource(R.string.hint_nota)) },
                    isError = notaError != null,
                    supportingText = notaError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = completado,
                        onCheckedChange = { completado = it }
                    )
                    Text(stringResource(R.string.check_completado))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Validaciones
                val tituloOk = titulo.isNotBlank()
                tituloError = if (!tituloOk) errorTituloText else null

                val anyoInt = anyo.toIntOrNull()
                val anyoOk = anyoInt != null && anyoInt in 1950..2100
                anyoError = if (!anyoOk) errorAnyoText else null

                val notaFloat = nota.replace(',', '.').toFloatOrNull()
                val notaOk = notaFloat != null && notaFloat in 0f..10f
                notaError = if (!notaOk) errorNotaText else null

                if (tituloOk && anyoInt != null && notaFloat != null) {
                    onSave(
                        Videojuego(
                            id = videojuegoInicial?.id ?: 0,
                            titulo = titulo.trim(),
                            plataforma = plataforma.trim(),
                            genero = genero.trim(),
                            anyo = anyoInt,
                            nota = notaFloat,
                            completado = completado
                        )
                    )
                }
            }) {
                Text(stringResource(R.string.boton_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.boton_cancelar))
            }
        }
    )
}
