package com.example.myapplication.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import com.example.myapplication.Ebook

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * EbookScreen: The main entry point for the E-book Library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbookScreen(viewModel: com.example.myapplication.TaskViewModel) {
    val context = LocalContext.current // Gets current Android context for asset access
    
    // INSTRUCTION: Get font size from ViewModel
    val ebookFontSize by viewModel.ebookFontSize.collectAsState()

    // Loads book data from assets/book.json and remembers it to avoid reloading on every frame
    val ebooks = remember {
        val jsonString = context.assets.open("book.json").bufferedReader().use { it.readText() } // Reads the file
        val type = object : TypeToken<List<Ebook>>() {}.type // Defines the data type for Gson
        Gson().fromJson<List<Ebook>>(jsonString, type) // Converts JSON string to Ebook objects
    }

    // State to hold the book currently being read (null = grid view, non-null = reader view)
    var selectedBook by remember { mutableStateOf<Ebook?>(null) }
    // State to hold the current search text typed by the user
    var searchQuery by remember { mutableStateOf("") }
    // State to hold the currently selected category for filtering (e.g., "All", "Prayer")
    var selectedCategory by remember { mutableStateOf("All") }
    // The list of available categories for the filter row
    val categories = listOf("All", "Prayer", "Story", "Song", "Anime")

    // Filters the ebooks list based on the search query (title or author) and category
    val filteredBooks = remember(searchQuery, selectedCategory) {
        ebooks.filter { book ->
            val matchesSearch = if (searchQuery.isEmpty()) true 
                               else book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
            val matchesCategory = if (selectedCategory == "All") true 
                                 else book.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    Scaffold( // Basic Material Design layout structure
        topBar = { // Defines the UI at the very top of the screen
            // Header container for title and search bar - Removed status bar padding to use the very top of the screen
            Column(
                modifier = Modifier.fillMaxWidth() // Takes full screen width
            ) {
                // Title of the library screen - Space reduced to use the top area efficiently
                Text(
                    text = "Book Library", // CHANGE: Change the library name here
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 30.sp,// CHANGE: Use headlineSmall for a prominent but compact title
                    fontWeight = FontWeight.Bold, // Makes title text bold
                    modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 2.dp) // Removed top/bottom padding
                )

                // The search input field
                OutlinedTextField(
                    value = searchQuery, // Displays current search state
                    onValueChange = { searchQuery = it }, // Updates state when typing
                    modifier = Modifier
                        .fillMaxWidth() // Bar takes full width
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    placeholder = { Text("Search books...", style = MaterialTheme.typography.bodyMedium) }, // Hint text
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) }, // Magnifying glass icon
                    shape = RoundedCornerShape(12.dp), // CHANGE: Adjust search bar corner roundness
                    singleLine = true, // Forces input to stay on one line
                    textStyle = MaterialTheme.typography.bodyMedium // Text style while typing
                )

                // Category Filter Row: A horizontally scrollable list of chips to filter books by type
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Space between each filter chip
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp) // External padding for the row
                ) {
                    items(categories) { category ->
                        // Individual filter chip for each category
                        FilterChip(
                            selected = selectedCategory == category, // Highlight if this category is active
                            onClick = { selectedCategory = category }, // Update filter state on click
                            label = { Text(category) } // Display category name
                        )
                    }
                }
            }
        }
    ) { padding -> // 'padding' is the space taken by the topBar
        // A grid layout that displays books in two columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // CHANGE: Set to 1 for a list or 3 for smaller cards
            contentPadding = PaddingValues(16.dp), // Padding around the entire grid
            horizontalArrangement = Arrangement.spacedBy(16.dp), // Space between columns
            verticalArrangement = Arrangement.spacedBy(16.dp), // Space between rows
            modifier = Modifier.padding(padding) // Applies scaffold padding to avoid overlap
        ) {
            // Loops through the filtered book list
            items(filteredBooks) { book ->
                // Renders each book card
                BookItem(book) { selectedBook = book } // When clicked, sets this book as the active reader book
            }
        }
    }

    // Conditional: If a book is selected, overlay the reader screen on top
    if (selectedBook != null) {
        EbookReader(selectedBook!!, ebookFontSize) { selectedBook = null } // 'onBack' sets selection to null to close it
    }
}

/**
 * BookItem: Represents a single book card in the library grid.
 */
@Composable
fun BookItem(book: Ebook, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    // The clickable card container for a single book
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Makes the whole card clickable
        shape = RoundedCornerShape(12.dp), // CHANGE: Adjust card corner roundness
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // CHANGE: Increase for more shadow
    ) {
        Column {
            // Container for the book cover image
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Takes full width of the card
                    .height(220.dp) // CHANGE: Adjust the height of book covers in the grid
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), // Clips image to top corners
                contentAlignment = Alignment.Center // Centers content if no image is found
            ) {
                if (book.coverImage.isNotEmpty()) {
                    val isSvgString = book.coverImage.trim().startsWith("<svg", ignoreCase = true)
                    
                    if (isSvgString) {
                        // Support for SVG strings directly in JSON
                        // We need to clean up escaped characters if they exist
                        val svgData = book.coverImage.replace("\\\"", "\"").replace("\\n", "\n")
                        val painter = rememberAsyncImagePainter(
                            model = svgData.toByteArray(),
                            imageLoader = imageLoader
                        )
                        Image(
                            painter = painter,
                            contentDescription = "Book Cover SVG",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        // Loads and displays the image file (PNG, JPG, or SVG file) from assets
                        AsyncImage(
                            model = "file:///android_asset/${book.coverImage}",
                            contentDescription = "Book Cover",
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                } else {
                    // Fallback visual if 'coverImage' is empty in book.json
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {}
                    Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            // Text area below the cover
            Column(modifier = Modifier.padding(12.dp)) {
                // Book Title
                Text(
                    text = book.title, 
                    fontWeight = FontWeight.Bold, // Bold font for title
                    maxLines = 1, // Cuts off text if too long
                    overflow = TextOverflow.Ellipsis // Adds "..." if text is cut off
                )
                // Author Name
                Text(
                    text = book.author, 
                    style = MaterialTheme.typography.bodySmall, // Smaller font for author
                    color = Color.Gray // CHANGE: Change color for author text
                )
            }
        }
    }
}

/**
 * Reads text from a .docx file by extracting content from word/document.xml.
 * Enhanced to support basic bold and italic formatting.
 */
private fun readDocxText(inputStream: java.io.InputStream): String {
    val zipInputStream = java.util.zip.ZipInputStream(inputStream)
    val sb = StringBuilder()
    try {
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xmlContent = zipInputStream.bufferedReader().readText()
                // Match paragraphs
                val pMatcher = java.util.regex.Pattern.compile("<w:p[^>]*>(.*?)</w:p>", java.util.regex.Pattern.DOTALL).matcher(xmlContent)
                while (pMatcher.find()) {
                    val pContent = pMatcher.group(1) ?: ""

                    // Match runs within paragraph
                    val rMatcher = java.util.regex.Pattern.compile("<w:r[^>]*>(.*?)</w:r>", java.util.regex.Pattern.DOTALL).matcher(pContent)
                    while (rMatcher.find()) {
                        val rContent = rMatcher.group(1) ?: ""
                        
                        // Check for bold and italic tags in run properties
                        val isBold = rContent.contains("<w:b/>") || rContent.contains("<w:b ")
                        val isItalic = rContent.contains("<w:i/>") || rContent.contains("<w:i ")

                        // Extract text from <w:t> tags
                        val tMatcher = java.util.regex.Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(rContent)
                        while (tMatcher.find()) {
                            var text = tMatcher.group(1) ?: ""
                            if (isBold) text = "<b>$text</b>"
                            if (isItalic) text = "<i>$text</i>"
                            sb.append(text)
                        }
                    }
                    sb.append("\n")
                }
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
    } catch (e: Exception) {
        return "Error parsing .docx: ${e.message}"
    } finally {
        zipInputStream.close()
    }
    return sb.toString()
}

/**
 * Cleans Word-exported HTML to be compatible with Android's AnnotatedString.fromHtml.
 * It preserves colors, bold, and italics.
 */
private fun cleanWordHtml(html: String): String {
    // 1. Extract content within <body> tags
    val bodyMatcher = java.util.regex.Pattern.compile("<body[^>]*>(.*?)</body>", java.util.regex.Pattern.DOTALL).matcher(html)
    var content = if (bodyMatcher.find()) bodyMatcher.group(1) else html

    // 2. Map specific Word classes to standard HTML tags for AnnotatedString
    content = content
        .replace("class=speaker", "style=\"font-weight:bold\"") // Speakers -> Bold
        .replace("class=headerh1", "style=\"font-size:24px; font-weight:bold\"") // Headers
        .replace("class=instruction", "style=\"font-style:italic; color:#666666\"") // Instructions -> Italics + Gray
        .replace("class=cross", "style=\"color:#FF0000; font-weight:bold\"") // Crosses -> Red Bold
        
    // Ensure styles that look like font-weight:bold are wrapped in <b> for better compatibility
    content = content.replace("style=\"[^\"]*font-weight:bold[^\"]*\"", "<b>")
        .replace("style='[^\"]*font-weight:bold[^\"]*'", "<b>")

    // 3. Convert Word's style='color:#RRGGBB' to <font color='#RRGGBB'>
    val colorRegex = "style='[^']*color:(#[0-9a-fA-F]{6})[^']*'".toRegex()
    content = content.replace(colorRegex) { matchResult ->
        "color=\"${matchResult.groupValues[1]}\""
    }

    // 4. Remove MS Word specific XML namespaces and tags
    content = content
        .replace("<o:p>.*?</o:p>".toRegex(), "")
        .replace("</?span[^>]*>".toRegex(), "")
        .replace("</?o:[^>]*>".toRegex(), "")
        .replace("<!\\[if !supportEmptyParas\\]>.*?<!\\[endif\\]>".toRegex(), "")
        .replace("&nbsp;", " ")

    return content
}

/**
 * EbookReader: A dedicated full-screen component for reading book content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbookReader(book: Ebook, fontSize: Float, onBack: () -> Unit) {
    val context = LocalContext.current
    // State to hold the actual content (resolved if it was a path)
    var displayContent by remember(book.content) { 
        mutableStateOf(if (book.content.endsWith(".docx") || book.content.contains("/")) "" else book.content)
    }

    // Effect to load content from assets if the content field is a path
    LaunchedEffect(book.content) {
        if (book.content.endsWith(".docx") || book.content.endsWith(".htm") || book.content.contains("/")) {
            try {
                val path = book.content
                context.assets.open(path).use { stream ->
                    displayContent = when {
                        path.endsWith(".docx") -> readDocxText(stream)
                        path.endsWith(".htm") -> cleanWordHtml(stream.bufferedReader().use { it.readText() })
                        else -> stream.bufferedReader().use { it.readText() }
                    }
                }
            } catch (e: Exception) {
                displayContent = "Error loading book content: ${e.message}"
            }
        }
    }

    // State to toggle between normal view and "Eye-Care" (Sepia) view
    var isReadingMode by remember { mutableStateOf(false) }
    // CHANGE: Edit these colors to customize the "Eye-Care" mode experience
    val backgroundColor = if (isReadingMode) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.surface
    val textColor = if (isReadingMode) Color(0xFF5B4636) else MaterialTheme.colorScheme.onSurface
    val authorColor = if (isReadingMode) Color(0xFF8B735B) else Color.Gray

    // Shows the reader in a full-screen window layer
    Dialog(
        onDismissRequest = onBack, // Action if user clicks outside
        properties = DialogProperties(usePlatformDefaultWidth = false) // Forces the dialog to be full screen width
    ) {
        Scaffold(
            topBar = {
                // Header layout for Back and Read Mode buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth() // Header width
                        .windowInsetsPadding(WindowInsets.statusBars) // Space for status bar icons
                        .padding(horizontal = 8.dp) // Side padding for buttons
                ) {
                    // The button used to close the reader
                    IconButton(
                        onClick = onBack, 
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.CenterStart) // Button area size
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Standard back arrow
                            contentDescription = "Close Reader", 
                            modifier = Modifier.size(30.dp), // CHANGE: Adjust back button icon size
                            tint = textColor // Uses dynamic color so it remains visible in all modes
                        )
                    }

                    // The button used to toggle Eye-Care (Sepia) mode
                    IconButton(
                        onClick = { isReadingMode = !isReadingMode }, // Swaps state on click
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.CenterEnd) // Positioned at top right
                    ) {
                        Icon(
                            imageVector = if (isReadingMode) Icons.Default.VisibilityOff else Icons.Default.Visibility, // Eye icon
                            contentDescription = "Toggle Reading Mode",
                            modifier = Modifier.size(30.dp), // CHANGE: Adjust eye icon size
                            tint = if (isReadingMode) Color(0xFFE67E22) else MaterialTheme.colorScheme.primary // CHANGE: Icon color
                        )
                    }
                }
            },
            containerColor = backgroundColor // Changes the background of the entire reader screen
        ) { padding ->
            // Container for the book text, makes it scrollable
            Column(
                modifier = Modifier
                    .fillMaxSize() // Fills screen
                    .padding(padding) // Avoids overlapping top bar
                    .padding(horizontal = 24.dp, vertical = 4.dp) // CHANGE: Adjust side margins for reading
                    .verticalScroll(rememberScrollState()) // Enables up/down scrolling
            ) {
                // Displays the Book Title at the top of the page
                Text(
                    text = book.title, 
                    style = MaterialTheme.typography.headlineMedium, // Large font style
                    fontWeight = FontWeight.Bold, // Bold text
                    textAlign = TextAlign.Center, // Centered title
                    color = textColor, // Dynamic color based on Read Mode
                    modifier = Modifier.fillMaxWidth() // Center within width
                )
                // Displays the Author name
                Text(
                    text = "by ${book.author}", 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = authorColor, // Dynamic color (brownish in sepia mode)
                    textAlign = TextAlign.Center, // Centered
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp) // Space below author
                )
                
                // Visual horizontal line separator
                HorizontalDivider(color = authorColor.copy(alpha = 0.3f)) 
                Spacer(modifier = Modifier.height(24.dp)) // Vertical gap after line
                
                // Main reading content text
                Text(
                    // Converts HTML tags (like <b>) and replaces line breaks with HTML breaks
                    text = if (book.content.endsWith(".htm")) {
                        AnnotatedString.fromHtml(displayContent)
                    } else {
                        AnnotatedString.fromHtml(displayContent.replace("\n", "<br>"))
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp), // CHANGE: Font size now controlled by settings
                    lineHeight = (fontSize * 1.5).sp, // CHANGE: Proportional line height for readability
                    textAlign = TextAlign.Justify, // Aligns text to both sides for a clean look
                    color = textColor // Dynamic color for comfortable reading
                )
                
                // Extra space at the very bottom so the last lines aren't cut off by screen edges
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
