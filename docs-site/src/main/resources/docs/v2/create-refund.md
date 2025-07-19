# Create a Refund

> **POST** `/v2/refunds`

Issue a full or partial refund for a previously successful payment. This is a critical function for managing customer returns, correcting billing errors, or handling disputes.

## 🎯 Overview

The Create Refund endpoint allows you to reverse a charge, returning funds to the customer. You can refund the entire amount of a payment or just a portion of it. Multiple partial refunds can be issued against a single payment, up to its original total amount.

**Key Features:**
- 💰 **Full & Partial Refunds**: Flexibility to refund any amount up to the original charge.
- 📝 **Reason Tracking**: Specify a reason for the refund for your internal records.
- 🔄 **Idempotency**: Safely retry refund requests without creating duplicate refunds.

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:
