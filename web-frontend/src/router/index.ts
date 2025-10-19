import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import SpeedTest from '../components/SpeedTest.vue'

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/test', name: 'test', component: SpeedTest },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
