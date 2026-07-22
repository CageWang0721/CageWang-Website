import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './styles/main.css'
import './styles/fluent.css'
import './styles/reference-admin.css'

import App from './App.vue'
import router from './router'

createApp(App)
  .use(createPinia())
  .use(router)
  .mount('#app')
