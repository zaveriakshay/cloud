# Retrieve a Refund

> **GET** `/v1/refunds/{refundId}`

Fetch the complete details of a single refund by its unique identifier. This is useful for checking the status of a specific refund or for auditing purposes.

## 🎯 Overview

The Retrieve Refund endpoint gives you a complete snapshot of a refund transaction. It returns the full refund object, including its current status, amount, reason, and a link back to the original payment.

**Key Features:**
- 🧾 **Complete Details**: Access every piece of information associated with a refund.
- 📊 **Status Check**: Programmatically verify if a refund has succeeded, is still pending, or has failed.
- 🔗 **Linked Payment**: Easily reference the original payment that was refunded.

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:

---

## 📋 Request Details

### Path Parameters

| Parameter  | Type   | Required       | Description                                                              |
|------------|--------|----------------|--------------------------------------------------------------------------|
| `refundId` | string | ✅ **Required** | The unique identifier of the refund to retrieve, starting with `re_`. |

---

## 📤 Response Details

### Success Response (200 OK)
