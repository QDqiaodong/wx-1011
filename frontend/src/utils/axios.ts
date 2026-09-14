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
    console.error('Axios error:', error)
    throw error
  }
)

export default instance