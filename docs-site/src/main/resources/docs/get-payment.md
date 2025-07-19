# Retrieve a Payment

> **GET** `/payments/{paymentId}`

Fetch the complete details of a single payment transaction by its unique identifier. This is useful for looking up specific transaction details, checking the status of a payment, or for customer support inquiries.

## 🎯 Overview

The Retrieve Payment endpoint gives you a snapshot of a payment at any time. It returns the full payment object, including its current status, amount, payment method details, and any associated metadata.

**Key Features:**
- 🧾 **Complete Details**: Access every piece of information associated with a payment.
- 📊 **Status Check**: Programmatically verify if a payment has succeeded, failed, or is pending.
- 🔗 **Linked Objects**: Includes details of related objects like the customer and payment method.

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:

---

## 📋 Request Details

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `paymentId` | string | ✅ **Required** | The unique identifier of the payment to retrieve, starting with `pay_`. |

---

## 📤 Response Details

### Success Response (200 OK)

### Response Fields
The response fields are identical to those returned when creating a payment.

---

## ⚠️ Error Responses

### 401 Unauthorized
**When:** Invalid or missing API key.

### 404 Not Found
**When:** No payment exists with the provided `paymentId`.