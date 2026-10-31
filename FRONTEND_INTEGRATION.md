# React Authentication Integration

Backend base URL: `http://localhost:8080`

## Suggested files

```text
src/
  services/
    api.js
    authService.js
  pages/
    Login.jsx
    Register.jsx
    ForgotPassword.jsx
    ResetPassword.jsx
  components/
    ProtectedRoute.jsx
```

## Axios client

```javascript
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" }
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;
```

## Auth service

```javascript
import api from "./api";

export const login = async (email, password) => {
  const response = await api.post("/auth/login", { email, password });
  sessionStorage.setItem("accessToken", response.data.accessToken);
  sessionStorage.setItem("role", response.data.role);
  return response.data;
};

export const register = (fullName, email, password) =>
  api.post("/auth/register", { fullName, email, password });

export const forgotPassword = (email) =>
  api.post("/auth/forgot-password", { email });

export const resetPassword = (token, newPassword) =>
  api.post("/auth/reset-password", { token, newPassword });

export const logout = () => {
  sessionStorage.removeItem("accessToken");
  sessionStorage.removeItem("role");
};
```

## Reset page

1. Read `token` from the URL query string with `URLSearchParams`.
2. Ask for a new password and confirmation.
3. Submit `{ token, newPassword }` to `POST /api/auth/reset-password`.
4. Show success and navigate to login.

## Role-based navigation

Use the `role` returned by login to show the appropriate menu. The backend must still enforce every permission with Spring Security.

`sessionStorage` is a prototype convenience, not an XSS-proof storage mechanism. For production, consider an HttpOnly, Secure, SameSite cookie design and revisit CSRF/CORS configuration.
