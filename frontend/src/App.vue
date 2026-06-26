<script setup>
import { ref, nextTick } from 'vue'
import axios from 'axios'

const messages = ref([
  { role: 'ai', text: '您好！我是勞基法問答機器人，請問有什麼勞資問題需要協助？' }
])
const input = ref('')
const loading = ref(false)
// 瀏覽器內建的，不需要安裝任何套件。每次開啟頁面產生一個新的 ID，代表一個新的對話
const conversationId = crypto.randomUUID()
const messagesEl = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

const send = async () => {
  const question = input.value.trim()
  if (!question || loading.value) return

  messages.value.push({ role: 'user', text: question })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await axios.post('http://localhost:8080/api/chat', { question, conversationId })
    messages.value.push({
      role: 'ai',
      text: res.data.answer,
      sources: res.data.sources
    })
  } catch {
    messages.value.push({ role: 'ai', text: '查詢失敗，請稍後再試。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <div class="app">
    <header>
      <h1>⚖️ 勞基法問答機器人</h1>
    </header>

    <div class="messages" ref="messagesEl">
      <div v-for="(msg, i) in messages" :key="i" :class="['bubble', msg.role]">
        {{ msg.text }}
        <div v-if="msg.sources && msg.sources.length" class="sources">
          <span class="sources-label">參考條文：</span>
          <span v-for="(s, j) in msg.sources" :key="j" class="source-tag">
            {{ s.lawName }} {{ s.articleNo }}
          </span>
        </div>
      </div>

      <div v-if="loading" class="bubble ai loading">AI 查詢中...</div>
    </div>

    <div class="input-area">
      <input v-model="input" @keyup.enter="send" placeholder="例如：老闆可以扣薪嗎？" :disabled="loading" />
      <button @click="send" :disabled="loading || !input.trim()">送出</button>
    </div>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 800px;
  margin: 0 auto;
  font-family: sans-serif;
}

header {
  padding: 16px;
  background: #1a56db;
  color: white;
}

header h1 {
  margin: 0;
  font-size: 20px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f5f5f5;
}

.bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 12px;
  white-space: pre-wrap;
  line-height: 1.6;
}

.bubble.user {
  align-self: flex-end;
  background: #1a56db;
  color: white;
  border-bottom-right-radius: 4px;
}

.bubble.ai {
  align-self: flex-start;
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.bubble.loading {
  color: #999;
}

.input-area {
  display: flex;
  padding: 12px;
  gap: 8px;
  background: white;
  border-top: 1px solid #ddd;
}

.input-area input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
}

.input-area input:focus {
  border-color: #1a56db;
}

.input-area button {
  padding: 10px 20px;
  background: #1a56db;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
}

.input-area button:disabled {
  background: #aaa;
  cursor: not-allowed;
}

.sources {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #eee;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.sources-label {
  font-size: 12px;
  color: #999;
}

.source-tag {
  font-size: 12px;
  background: #f0f4ff;
  color: #1a56db;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid #c7d7f9;
}
</style>
