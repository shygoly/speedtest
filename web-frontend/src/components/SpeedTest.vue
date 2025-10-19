<template>
  <div class="container">
    <h2>Web 端测速（占位）</h2>
    <p>前端通过 WebSocket 与服务端握手，UDP 由移动端/PC 客户端承载。此处展示 WebSocket 连通性与结果展示。</p>

    <div style="margin: 16px 0">
      <label>服务端地址：</label>
      <input v-model="host" placeholder="ws://example.com:8080" style="width: 320px" />
      <button @click="connect" :disabled="connecting" style="margin-left: 8px">{{ connecting ? '连接中...' : '连接' }}</button>
    </div>

    <div v-if="log.length" style="background:#111;color:#0f0;padding:12px;border-radius:6px;min-height:120px;white-space:pre-wrap">
      <div v-for="(l,i) in log" :key="i">{{ l }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const host = ref('ws://localhost:8080')
const connecting = ref(false)
const log = ref<string[]>([])
let ws: WebSocket | null = null

function push(msg: string) {
  log.value.push(new Date().toLocaleTimeString() + ' ' + msg)
}

function connect() {
  if (!host.value) return
  if (ws) { ws.close(); ws = null }
  connecting.value = true
  try {
    ws = new WebSocket(host.value.replace('http', 'ws'))
    ws.onopen = () => { push('WebSocket 已连接'); connecting.value = false; ws?.send(JSON.stringify({ msg: 'hello' })) }
    ws.onmessage = (ev) => push('收到: ' + ev.data)
    ws.onerror = () => { push('WebSocket 错误'); connecting.value = false }
    ws.onclose = () => { push('WebSocket 已关闭'); connecting.value = false }
  } catch (e: any) {
    push('连接异常: ' + e.message)
    connecting.value = false
  }
}
</script>

<style scoped>
input{padding:6px;border:1px solid #ccc;border-radius:4px}
button{padding:6px 12px}
</style>
