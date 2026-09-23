// ============================================================================
// APP — the route table. React Router maps URL -> page component.
// ============================================================================
// STRUCTURE:
//   <Routes>
//     /login          -> LoginPage                    (public)
//     / (Layout)      -> shell with nav bar + <Outlet/>
//        index        -> DashboardPage                (protected)
//        transfer     -> TransferPage                 (protected)
//        statements   -> StatementsPage               (protected)
//        loans        -> LoansPage                    (protected)
//        *            -> redirect to /                (404 catch-all)
//
// Every protected route is wrapped in <RequireAuth>: no session -> bounce to
// /login. NESTED routes under Layout give all pages the shared nav for free.

import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { RequireAuth } from './auth/RequireAuth'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { TransferPage } from './pages/TransferPage'
import { StatementsPage } from './pages/StatementsPage'
import { LoansPage } from './pages/LoansPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="transfer" element={<TransferPage />} />
        <Route path="statements" element={<StatementsPage />} />
        <Route path="loans" element={<LoansPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
