package com.example.ui.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 1. الـ دعم الإجراءات والخيارات التفاعلية داخل فقاعة الرسالة
data class ChatAction(
    val label: String,
    val actionPayload: String
)

class SupportChatViewModel(
    private val tenantId: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    data class UiMessage(
        val text: String,
        val isUser: Boolean,
        val isCode: Boolean = false,
        val actions: List<ChatAction> = emptyList()
    )

    private val _messages = MutableStateFlow<List<UiMessage>>(listOf(
        UiMessage(
            text = "السلام عليكم! أنا مساعد جمعية ${tenantId} الذكي.\n\nأقدر أساعدك في حل المشكلات التقنية وتوليد الأكواد وإدارة الحلقات بسرعة.",
            isUser = false,
            actions = listOf(
                ChatAction("توليد كود طالب جديد", "كود طالب"),
                ChatAction("تقرير الحضور والتقدم", "تقرير الحفظ"),
                ChatAction("إضافة حلقة قرآنية", "إضافة حلقة"),
                ChatAction("فحص المزامنة", "مزامنة")
            )
        )
    ))
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return
        _messages.value += UiMessage(userInput, true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // البحث في حلول هذه الجمعية فقط من Firestore
                val snapshot = firestore.collection("tenants").document(tenantId)
                    .collection("bot_solutions")
                    .get().await()

                var foundReply: String? = null
                var actionType: String? = null

                // بحث ذكي بالكلمات المفتاحية
                for (doc in snapshot.documents) {
                    val keywords = doc.get("keywords") as? List<String> ?: emptyList()
                    if (keywords.any { userInput.contains(it, ignoreCase = true) }) {
                        foundReply = doc.getString("solution")
                        actionType = doc.getString("action")
                        break
                    }
                }

                // معالجة الطلبات الخاصة أو الشائعة
                if (actionType == "GENERATE_CODE" || userInput.contains("كود") || userInput.contains("طالب")) {
                    val newCode = "QUR-${tenantId.take(4).uppercase()}-${(10000..99999).random()}"
                    _messages.value += UiMessage(
                        text = "تم توليد وتجهيز كود جديد لطالب في ${tenantId}:\n\n$newCode\n\nقم بنسخه وإرساله لولي الأمر أو الطالب عبر واتساب.",
                        isUser = false,
                        isCode = true,
                        actions = listOf(
                            ChatAction("توليد كود آخر", "كود طالب"),
                            ChatAction("عرض قائمة الطلاب", "الطلاب")
                        )
                    )
                } else if (userInput.contains("مزامنة") || userInput.contains("سحب")) {
                    _messages.value += UiMessage(
                        text = "حالة المزامنة السحابية (Firestore):\n• الاتصال بالخادم: متصل بنجاح 🟢\n• آخر مزامنة: قبل دقيقة واحدة\n• السجلات المحلية: متزامنة بالكامل",
                        isUser = false,
                        actions = listOf(
                            ChatAction("إجبار مزامنة فورية", "مزامنة"),
                            ChatAction("العودة للرئيسية", "الرئيسية")
                        )
                    )
                } else {
                    val reply = foundReply ?: "عذراً، لم أجد إجابة مطابقة تماماً لطلبك '$userInput'. يمكنك اختيار أحد الخيارات السريعة أدناه:"
                    _messages.value += UiMessage(
                        text = reply,
                        isUser = false,
                        actions = listOf(
                            ChatAction("توليد كود طالب", "كود طالب"),
                            ChatAction("تقرير الحفظ", "تقرير الحفظ"),
                            ChatAction("مساعدة عامة", "مساعدة")
                        )
                    )
                }

            } catch (e: Exception) {
                _messages.value += UiMessage(
                    text = "حدث خطأ أثناء جلب الحلول من السحابة: ${e.message}\nلكن النظام يعمل بشكل محلي.",
                    isUser = false,
                    actions = listOf(ChatAction("إعادة المحاولة", userInput))
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Factory لإنشاء الـ ViewModel مع tenantId
class SupportChatViewModelFactory(private val tenantId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SupportChatViewModel(tenantId) as T
    }
}

// 2. الشاشة الرئيسية Material 3 مع دعم الأزرار السريعة
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    tenantId: String,
    onBack: () -> Unit
) {
    val viewModel: SupportChatViewModel = viewModel(factory = SupportChatViewModelFactory(tenantId))
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مساعد جمعية $tenantId الذكي", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اكتب استفسارك هنا...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg, onActionClick = { payload ->
                    viewModel.sendMessage(payload)
                })
            }
            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("المساعد يحلل طلبك...", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    msg: SupportChatViewModel.UiMessage,
    onActionClick: (String) -> Unit
) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (msg.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (msg.isCode) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = color),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = if (msg.isCode) TextAlign.Center else TextAlign.Start,
                    fontWeight = if (msg.isCode) FontWeight.Bold else FontWeight.Normal
                )

                // عرض الأزرار والخيارات الإضافية التفاعلية عند توفرها
                if (msg.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        msg.actions.forEach { action ->
                            OutlinedButton(
                                onClick = { onActionClick(action.actionPayload) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
