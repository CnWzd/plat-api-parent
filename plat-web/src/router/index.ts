import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { guest: true },
    },
    // ---------------- 开发者门户 ----------------
    {
      path: '/portal',
      component: () => import('@/layouts/PortalLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/portal/dashboard' },
        { path: 'dashboard', name: 'portal-dashboard', component: () => import('@/views/portal/Dashboard.vue') },
        { path: 'apps', name: 'portal-apps', component: () => import('@/views/portal/Apps.vue') },
        { path: 'apps/:id', name: 'portal-app-detail', component: () => import('@/views/portal/AppDetail.vue') },
        { path: 'statistics', name: 'portal-statistics', component: () => import('@/views/portal/Statistics.vue') },
      ],
    },
    // ---------------- 运营管理后台 ----------------
    {
      path: '/console',
      component: () => import('@/layouts/ConsoleLayout.vue'),
      meta: { requiresAuth: true, adminOnly: true },
      children: [
        { path: '', redirect: '/console/apis' },
        { path: 'apis', name: 'console-apis', component: () => import('@/views/console/Apis.vue') },
        { path: 'approvals', name: 'console-approvals', component: () => import('@/views/console/Approvals.vue') },
        { path: 'apps', name: 'console-apps', component: () => import('@/views/console/ConsoleApps.vue') },
        { path: 'tenants', name: 'console-tenants', component: () => import('@/views/console/Tenants.vue') },
        { path: 'users', name: 'console-users', component: () => import('@/views/console/Users.vue') },
        { path: 'statistics', name: 'console-statistics', component: () => import('@/views/console/Statistics.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.adminOnly && !auth.isAdmin) {
    return { path: '/portal/dashboard' }
  }
  if (to.meta.guest && auth.isLoggedIn) {
    return { path: auth.homePath() }
  }
  return true
})

export default router
