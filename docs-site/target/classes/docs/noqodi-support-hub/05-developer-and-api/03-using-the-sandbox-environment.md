[< Back to Support Hub](../index.md)

# How to Use the PayPro Sandbox for Testing

The PayPro Sandbox is a complete, self-contained testing environment that mirrors our live production system. It allows you to build and test your integration without touching real money or your live account data.

### Key Features of the Sandbox
* Uses a separate set of [API Keys](./01-getting-your-api-keys.md) (your test keys).
* Has its own isolated data. Customers and transactions created in the sandbox do not exist in your live account.
* Provides special test card numbers that can be used to simulate different payment scenarios.

### Step 1: Access Your Test Data
In your **Developer Dashboard**, use the toggle in the left-hand menu to switch from "Live" to **"Test Mode"**. When in Test Mode, all the data you see (payments, customers, etc.) is your sandbox data.

![Screenshot of the Live/Test mode toggle in the developer dashboard.](https://placehold.co/800x300/E8E8E8/2E2E2E?text=Live/Test+Mode+Toggle)

### Step 2: Use Your Test API Keys
When your application is running in a development or testing environment, make sure it is configured to use your **Test Publishable Key** and **Test Secret Key**.

### Step 3: Simulate Payments with Test Cards
You cannot use real credit cards in the sandbox. Instead, we provide a set of special card numbers that can be used to trigger specific responses.

Here are some common test cards:

| Card Number       | Expiration | CVC | Result              |
| ----------------- | ---------- | --- | ------------------- |
| `4242 4242 4242 4242` | Any future | Any | **Success** |
| `4000 0000 0000 0002` | Any future | Any | **Failed - Insufficient Funds** |
| `4111 1111 1111 1111` | Any future | Any | **Failed - Incorrect CVC** |

Use these numbers in your checkout form when testing your integration. You can find a full list of test cards in our official API documentation.

By thoroughly testing in the sandbox, you can ensure your integration is robust and reliable before you go live and start processing real payments.