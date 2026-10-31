# Brightloop Authentication — Thunder Client Testing Guide ⚡

Aapke project mein Thunder Client testing ke liye direct import files add kar di gayi hain:
1. `thunder-collection_brightloop-auth.json` (Saare 10 Ready-to-use API Requests)
2. `thunder-environment_brightloop-local.json` (Environment variables)

---

## 🚀 Step 1: Thunder Client Open & Collection Import Karein

1. VS Code / IDE mein left sidebar par **Thunder Client** icon (⚡) par click karein.
2. Left menu mein **"Collections"** tab par jayein.
3. Top right mein 3 dots (`...`) ya menu par click karein aur **"Import"** select karein.
4. Apne project folder se `thunder-collection_brightloop-auth.json` select karein.
5. **Boom!** "Brightloop Authentication API" folder aapke Thunder Client mein aa jayega jisme 10 pre-configured requests hain:
   - 📁 **1. Authentication**
     - `1. Register Customer`
     - `2. Login as Admin`
     - `3. Login as Customer`
   - 📁 **2. Role-Based Access Control (RBAC)**
     - `4. Get Current User (/api/me)`
     - `5. Customer Access Check`
     - `6. Staff Access Check`
     - `7. Admin Access Check`
   - 📁 **3. Admin Management**
     - `8. Admin Create Staff Account`
   - 📁 **4. Password Reset**
     - `9. Forgot Password (Send Reset Email)`
     - `10. Reset Password (with Token)`

---

## ⚡ Step 2: Testing Flow (Ek Ek Karke Kaise Test Karein)

### Test 1: Admin Login
- Request: `2. Login as Admin`
- **Method:** POST `http://localhost:8080/api/auth/login`
- Body already set hai:
  ```json
  {
    "email": "admin@brightloop.com",
    "password": "Admin@1234"
  }
  ```
- **"Send"** dabayein.
- **Response:** `200 OK` aayega jisme `accessToken` milega.
- Us `accessToken` ko copy kar lein!

---

### Test 2: Protected Profile Check (`/api/me`)
- Request: `4. Get Current User (/api/me)`
- **Headers** tab mein jayein:
  - Header: `Authorization`
  - Value: `Bearer <paste_your_copied_accessToken_here>`
- **"Send"** dabayein.
- **Response:** `200 OK`
  ```json
  {
    "id": "f70c5a10-0ee5-4c42-a7bc-22874c778cf7",
    "fullName": "Brightloop Admin",
    "email": "admin@brightloop.com",
    "role": "ADMIN"
  }
  ```

---

### Test 3: Customer Register & Login
1. Open `1. Register Customer`
   - **Send** karein -> Response: `"Customer account created successfully."`
2. Open `3. Login as Customer`
   - **Send** karein -> Response: Naya customer token aayega jisme `"role": "CUSTOMER"` hoga.

---

### Test 4: Role-Based Access Control (RBAC Security Test)
- Customer ke Token ke saath:
  - `5. Customer Access Check` -> **200 OK** (`CUSTOMER access granted`)
  - `7. Admin Access Check` -> **403 Forbidden** (System customer ko admin route access nahi karne dega!)
- Admin ke Token ke saath:
  - `7. Admin Access Check` -> **200 OK** (`ADMIN access granted`)

---

### Test 5: Admin Creates Staff Account
- Request: `8. Admin Create Staff Account`
- Headers: Admin ka `Authorization: Bearer <admin_token>`
- **Send** karein -> Response: `"Staff account created successfully."`
- Staff login karke `6. Staff Access Check` test kar sakte hain!

---

### Test 6: Forgot Password (Real Gmail SMTP Test)
- Request: `9. Forgot Password`
- Body:
  ```json
  {
    "email": "kmp8107@gmail.com"
  }
  ```
- **Send** karein -> Response:
  ```json
  {
    "message": "If an account exists for that email, a password reset link has been sent."
  }
  ```
- Aapke Gmail inbox (`kmp8107@gmail.com`) par password reset link ka real email aayega!
- Email se token copy karke `10. Reset Password` request mein paste karke naya password set kar sakte hain.

---

## 🛠️ App Restart Command (Agar future mein dobara chalana ho)
Jab bhi project run karna ho, terminal mein bas yeh command chalayein:
```powershell
.\mvnw.cmd spring-boot:run
```