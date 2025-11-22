import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Users from './pages/admin/Users'
import Roles from './pages/admin/Roles'
import PageAuthorizations from './pages/admin/PageAuthorizations'
import UserAuthorizations from './pages/admin/UserAuthorizations'
import MasterData from './pages/admin/MasterData'
import BusinessRules from './pages/admin/BusinessRules'
import Projects from './pages/projects/Projects'
import WbsHierarchy from './pages/projects/WbsHierarchy'
import Tasks from './pages/projects/Tasks'
import TaskUpdates from './pages/projects/TaskUpdates'
import ConfirmationPage from './pages/projects/ConfirmationPage'
import PlanVersions from './pages/projects/PlanVersions'
import ResourceAllocationPage from './pages/projects/ResourceAllocationPage'
import ProtectedRoute from './components/ProtectedRoute'
import TopBar from './components/TopBar'
import RuleViolationModal from './components/RuleViolationModal'

function App() {
  return (
    <AuthProvider>
      <Router>
        <RuleViolationModal />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <TopBar />
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute>
                <TopBar />
                <Users />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/roles"
            element={
              <ProtectedRoute>
                <TopBar />
                <Roles />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/page-authorizations"
            element={
              <ProtectedRoute>
                <TopBar />
                <PageAuthorizations />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/user-authorizations"
            element={
              <ProtectedRoute>
                <TopBar />
                <UserAuthorizations />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/master-data"
            element={
              <ProtectedRoute>
                <TopBar />
                <MasterData />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/business-rules"
            element={
              <ProtectedRoute>
                <TopBar />
                <BusinessRules />
              </ProtectedRoute>
            }
          />
          <Route
            path="/projects"
            element={
              <ProtectedRoute>
                <TopBar />
                <Projects />
              </ProtectedRoute>
            }
          />
          <Route
            path="/projects/:projectId/wbs"
            element={
              <ProtectedRoute>
                <TopBar />
                <WbsHierarchy />
              </ProtectedRoute>
            }
          />
          <Route
            path="/wbs/:wbsId/tasks"
            element={
              <ProtectedRoute>
                <TopBar />
                <Tasks />
              </ProtectedRoute>
            }
          />
          <Route
            path="/wbs/:wbsId/confirmations"
            element={
              <ProtectedRoute>
                <TopBar />
                <ConfirmationPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/wbs/:wbsId/resources"
            element={
              <ProtectedRoute>
                <TopBar />
                <ResourceAllocationPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tasks/:taskId/updates"
            element={
              <ProtectedRoute>
                <TopBar />
                <TaskUpdates />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tasks/:taskId/plans"
            element={
              <ProtectedRoute>
                <TopBar />
                <PlanVersions />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  )
}

export default App

