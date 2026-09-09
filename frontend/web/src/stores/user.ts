import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, type LoginParams, type LoginResult } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref('')
  const username = ref('')
  const permissions = ref<string[]>([])

  async function login(params: LoginParams) {
    const data = await loginApi(params)
    token.value = data.token
    role.value = data.role
    username.value = data.username
    permissions.value = data.permissions
    localStorage.setItem('token', data.token)
    return data
  }

  function setProfile(p: { role: string; username: string; permissions: string[] }) {
    role.value = p.role
    username.value = p.username
    permissions.value = p.permissions
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略网络错误，本地状态仍清空
    }
    token.value = ''
    role.value = ''
    username.value = ''
    permissions.value = []
    localStorage.removeItem('token')
  }

  return { token, role, username, permissions, login, setProfile, logout }
})
