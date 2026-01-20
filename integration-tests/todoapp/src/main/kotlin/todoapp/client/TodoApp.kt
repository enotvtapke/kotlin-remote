package todoapp.client

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import kotlinx.remote.asContext
import todoapp.model.CreateTodoRequest
import todoapp.model.Todo
import todoapp.model.UpdateTodoRequest
import todoapp.remote.ServerConfig
import todoapp.server.createTodo
import todoapp.server.deleteTodo
import todoapp.server.todos
import todoapp.server.updateTodo

// Color Palette - Deep Ocean Theme
private val DeepOceanDark = Color(0xFF0A1628)
private val DeepOceanMid = Color(0xFF152238)
private val DeepOceanLight = Color(0xFF1E3A5F)
private val CoralAccent = Color(0xFFFF6B6B)
private val TealAccent = Color(0xFF4ECDC4)
private val GoldAccent = Color(0xFFFFE66D)
private val TextPrimary = Color(0xFFF8F9FA)
private val TextSecondary = Color(0xFFADB5BD)
private val TextMuted = Color(0xFF6C757D)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Todo App",
        state = rememberWindowState(width = 520.dp, height = 740.dp)
    ) {
        TodoApp()
    }
}

@Composable
fun TodoApp() {
    val scope = rememberCoroutineScope()
    var todoList by remember { mutableStateOf<List<Todo>>(emptyList()) }
    var newTodoTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    fun loadTodos() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                context(ServerConfig.asContext()) {
                    todoList = todos()
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load todos: ${e.message}"
            }
            isLoading = false
        }
    }

    fun addTodo() {
        if (newTodoTitle.isBlank()) return
        scope.launch {
            try {
                context(ServerConfig.asContext()) {
                    createTodo(CreateTodoRequest(newTodoTitle.trim()))
                    newTodoTitle = ""
                    todoList = todos()
                }
            } catch (e: Exception) {
                errorMessage = "Failed to add todo: ${e.message}"
            }
        }
    }

    fun toggleTodo(todo: Todo) {
        scope.launch {
            try {
                context(ServerConfig.asContext()) {
                    updateTodo(todo.id, UpdateTodoRequest(done = !todo.done))
                    todoList = todos()
                }
            } catch (e: Exception) {
                errorMessage = "Failed to update todo: ${e.message}"
            }
        }
    }

    fun removeTodo(todo: Todo) {
        scope.launch {
            try {
                context(ServerConfig.asContext()) {
                    deleteTodo(todo.id)
                    todoList = todos()
                }
            } catch (e: Exception) {
                errorMessage = "Failed to delete todo: ${e.message}"
            }
        }
    }

    fun updateTitle(todo: Todo, newTitle: String) {
        if (newTitle.isBlank() || newTitle == todo.title) return
        scope.launch {
            try {
                context(ServerConfig.asContext()) {
                    updateTodo(todo.id, UpdateTodoRequest(title = newTitle.trim()))
                    todoList = todos()
                }
            } catch (e: Exception) {
                errorMessage = "Failed to update todo: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTodos()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepOceanDark, DeepOceanMid, DeepOceanDark)
                )
            )
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(TealAccent.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CoralAccent.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TASKS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = TealAccent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What needs to be done?",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .background(DeepOceanLight, RoundedCornerShape(16.dp))
                    .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = newTodoTitle,
                    onValueChange = { newTodoTitle = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                                addTodo()
                                true
                            } else false
                        },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Default
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (newTodoTitle.isEmpty()) {
                                Text(
                                    "Add a new task...",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        color = TextMuted
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                IconButton(
                    onClick = { addTodo() },
                    modifier = Modifier
                        .padding(4.dp)
                        .size(48.dp)
                        .background(
                            Brush.horizontalGradient(listOf(TealAccent, TealAccent.copy(alpha = 0.8f))),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = DeepOceanDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val completedCount = todoList.count { it.done }
                val totalCount = todoList.size
                
                Text(
                    text = "$completedCount of $totalCount completed",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                )
                
                IconButton(
                    onClick = { loadTodos() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                errorMessage?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        color = CoralAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(12.dp),
                            style = TextStyle(fontSize = 13.sp, color = CoralAccent)
                        )
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TealAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (todoList.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "~",
                            style = TextStyle(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Thin,
                                color = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tasks yet",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = TextMuted
                            )
                        )
                        Text(
                            text = "Add your first task above",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = TextMuted.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            } else {
                // Todo list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(todoList.sortedByDescending { it.createdAt }, key = { it.id }) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggle = { toggleTodo(todo) },
                            onDelete = { removeTodo(todo) },
                            onUpdateTitle = { newTitle -> updateTitle(todo, newTitle) }
                        )
                    }
                }
            }

            // Footer
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter to add • Click to expand • Double-click to edit",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = TextMuted.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onUpdateTitle: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(todo.title) { mutableStateOf(todo.title) }
    val editFocusRequester = remember { FocusRequester() }
    
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1.01f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )

    LaunchedEffect(isEditing) {
        if (isEditing) {
            editFocusRequester.requestFocus()
        }
    }

    fun saveEdit() {
        if (editText.isNotBlank() && editText != todo.title) {
            onUpdateTitle(editText)
        } else {
            editText = todo.title
        }
        isEditing = false
    }

    fun cancelEdit() {
        editText = todo.title
        isEditing = false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(if (isExpanded) 4.dp else 2.dp, RoundedCornerShape(12.dp))
            .background(
                when {
                    isEditing -> DeepOceanLight.copy(alpha = 1f)
                    todo.done -> DeepOceanMid.copy(alpha = 0.6f)
                    else -> DeepOceanLight
                },
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                when {
                    isEditing -> GoldAccent.copy(alpha = 0.5f)
                    todo.done -> Color.Transparent
                    else -> TealAccent.copy(alpha = 0.15f)
                },
                RoundedCornerShape(12.dp)
            )
            .clickable { if (!isEditing) isExpanded = !isExpanded }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (todo.done)
                        TealAccent
                    else
                        Color.Transparent
                )
                .border(
                    2.dp,
                    if (todo.done) Color.Transparent else TextMuted,
                    CircleShape
                )
                .clickable { if (!isEditing) onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (todo.done) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = DeepOceanDark,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title / Edit field
        Column(modifier = Modifier.weight(1f)) {
            if (isEditing) {
                BasicTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(editFocusRequester)
                        .onKeyEvent { event ->
                            when {
                                event.key == Key.Enter && event.type == KeyEventType.KeyUp -> {
                                    saveEdit()
                                    true
                                }
                                event.key == Key.Escape && event.type == KeyEventType.KeyUp -> {
                                    cancelEdit()
                                    true
                                }
                                else -> false
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepOceanMid, RoundedCornerShape(6.dp))
                                .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            innerTextField()
                        }
                    }
                )
                Text(
                    text = "Enter to save • Esc to cancel",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = GoldAccent.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = todo.title,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = if (todo.done) FontWeight.Normal else FontWeight.Medium,
                        color = if (todo.done) TextMuted else TextPrimary,
                        textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    modifier = Modifier.clickable(
                        interactionSource = null,
                        indication = null
                    ) { 
                        isEditing = true 
                    }
                )
                Text(
                    text = todo.createdAt.let { "${it.dayOfMonth}/${it.monthNumber}/${it.year} at ${it.hour}:${it.minute.toString().padStart(2, '0')}" },
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = TextMuted.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        // Action buttons
        AnimatedVisibility(
            visible = isEditing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Row {
                IconButton(
                    onClick = { saveEdit() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        tint = TealAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { cancelEdit() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = CoralAccent.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isEditing && (isExpanded || todo.done),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Row {
                IconButton(
                    onClick = { isEditing = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = GoldAccent.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CoralAccent.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

