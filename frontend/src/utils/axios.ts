import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.response.use(
  response => {
    const result = response.data
    if (result.code === 200) {
      return result.data
    } else {
      throw new Error(result.message || '请求失败')
    }
  },
  error => {
    // 后端业务失败（如 409 重复封账、404 未封账）仍返回统一 Result 结构，
    // 这里取出 message，保证页面能显示后端给出的确切失败原因。
    const body = error.response?.data
    if (body && typeof body === 'object' && body.message) {
      const err = new Error(body.message) as Error & { code?: number; status?: number }
      err.code = body.code
      err.status = error.response?.status
      return Promise.reject(err)
    }
    console.error('Axios error:', error)
    return Promise.reject(error)
  }
)

export default instance
