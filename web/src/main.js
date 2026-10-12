import { createApp } from 'vue'
import { Button, Cell, CellGroup, Field, Form, NavBar, Tabbar, TabbarItem, List, PullRefresh, Dialog, Tag, Card, Steps, Step } from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(router)
app.use(Button).use(Cell).use(CellGroup).use(Field).use(Form)
   .use(NavBar).use(Tabbar).use(TabbarItem).use(List).use(PullRefresh)
   .use(Dialog).use(Tag).use(Card).use(Steps).use(Step)
app.mount('#app')
