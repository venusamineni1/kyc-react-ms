import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';
import Layout from './components/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ClientDirectory from './pages/ClientDirectory';
import ClientDetails from './pages/ClientDetails';
import CaseList from './pages/CaseList';
import CaseDetails from './pages/CaseDetails';
import MaterialChanges from './pages/MaterialChanges';
import AdminUserManagement from './pages/AdminUserManagement';
import AdminPermissions from './pages/AdminPermissions';
import AdminQuestionnaire from './pages/AdminQuestionnaire';
import AdminWorkflowDashboard from './pages/AdminWorkflowDashboard';
import Configuration from './pages/Configuration';
import AuditHistory from './pages/AuditHistory';
import Questionnaire from './pages/Questionnaire';
import TaskInbox from './pages/TaskInbox';
import AdHocTaskList from './pages/AdHocTaskList';
import AdminMaterialConfig from './pages/AdminMaterialConfig';
import BatchMappingConfig from './pages/BatchMappingConfig';
import BatchRiskMappingConfig from './pages/BatchRiskMappingConfig';
import ServiceStatus from './pages/ServiceStatus';
import BatchPipeline from './pages/BatchPipeline';
import BatchRiskPipeline from './pages/BatchRiskPipeline';
import History from './pages/History';


import Profile from './pages/Profile';

// Placeholder components for the other pages
const Placeholder = ({ name }) => (
  <div className="glass-section">
    <h2>{name}</h2>
    <p>This page is under migration.</p>
    <Link to="/" className="back-link">Back to Dashboard</Link>
  </div>
);

import { Link } from 'react-router-dom';

import ProtectedRoute from './components/ProtectedRoute'; // Import usage

import { InboxProvider } from './contexts/InboxContext';
import { NotificationProvider } from './contexts/NotificationContext';
import ToastContainer from './components/ToastContainer';

function App() {
  return (
    <AuthProvider>
      <ThemeProvider>
        <NotificationProvider>
          <InboxProvider>
            <ToastContainer />
            <Router>
              <Routes>
                {/* Public Route */}
                <Route path="/login" element={<Login />} />

                {/* Protected Routes - utilizing Layout inside ProtectedRoute or wrapping Layout */}
                <Route
                  path="*"
                  element={
                    <ProtectedRoute>
                      <Layout>
                        <Routes>
                          <Route path="/" element={<Dashboard />} />
                          <Route path="/clients" element={<ClientDirectory />} />
                          <Route path="/clients/:id" element={<ClientDetails />} />
                          <Route path="/changes" element={<MaterialChanges />} />
                          <Route path="/users" element={<AdminUserManagement />} />
                          <Route path="/permissions" element={<AdminPermissions />} />
                          <Route path="/cases" element={<CaseList />} />
                          <Route path="/cases/:id" element={<CaseDetails />} />
                          <Route path="/cases/:id/questionnaire" element={<Questionnaire />} />
                          <Route path="/admin/questionnaire" element={<AdminQuestionnaire />} />
                          <Route path="/admin/workflow" element={<AdminWorkflowDashboard />} />
                          <Route path="/admin/audits" element={
                            <ProtectedRoute requiredPermission="MANAGE_AUDITS">
                              <AuditHistory />
                            </ProtectedRoute>
                          } />
                          <Route path="/admin/config" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <Configuration />
                            </ProtectedRoute>
                          } />
                          <Route path="/admin/material-configs" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <AdminMaterialConfig />
                            </ProtectedRoute>
                          } />
                          <Route path="/admin/batch-mapping" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <BatchMappingConfig />
                            </ProtectedRoute>
                          } />
                          <Route path="/admin/risk-mapping" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <BatchRiskMappingConfig />
                            </ProtectedRoute>
                          } />
                          <Route path="/admin/services" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <ServiceStatus />
                            </ProtectedRoute>
                          } />
                          <Route path="/admin/batch-pipeline" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <BatchPipeline />
                            </ProtectedRoute>
                          } />
                          <Route path="/admin/risk-pipeline" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <BatchRiskPipeline />
                            </ProtectedRoute>
                          } />
                          <Route path="/history" element={
                            <ProtectedRoute requiredPermission="MANAGE_CONFIG">
                              <History />
                            </ProtectedRoute>
                          } />
                          <Route path="/inbox" element={<TaskInbox />} />
                          <Route path="/adhoc-tasks" element={<AdHocTaskList />} />
                          <Route path="/profile" element={<Profile />} />
                        </Routes>
                      </Layout>
                    </ProtectedRoute>
                  }
                />
              </Routes>
            </Router>
          </InboxProvider>
        </NotificationProvider>
      </ThemeProvider>
    </AuthProvider >
  );
}

export default App;
