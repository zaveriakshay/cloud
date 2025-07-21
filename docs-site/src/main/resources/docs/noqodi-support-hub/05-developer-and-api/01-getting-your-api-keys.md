[< Back to Support Hub](../index.md)

# Getting Your API Keys for Integration

To integrate PayPro with your website or application, you need API (Application Programming Interface) keys. These keys act like a username and password for your application, allowing it to securely communicate with our servers.

There are two types of keys:
* **Publishable Key:** This key is meant to be used in your client-side code (like your website's JavaScript). It can be used to create payment tokens but cannot perform sensitive actions.
* **Secret Key:** This key is for your server-side code (e.g., PHP, Node.js, Python). **It must be kept confidential and never exposed to the public.** It is used to authorize payments and other sensitive API calls.

### How to Find Your API Keys

1.  Log in to your PayPro **Developer Dashboard**. This is separate from your main account dashboard.
2.  Navigate to the **"API Keys"** section in the left-hand menu.

![Screenshot of the Developer Dashboard with the API Keys section highlighted.](https://placehold.co/800x450/E8E8E8/2E2E2E?text=Developer+Dashboard+API+Keys)

3.  Your **Publishable** and **Secret** keys will be displayed on this page. You can click to copy them.

> **Security Warning:** Your Secret Key provides full access to your account's payment functions. Treat it like a password. Do not commit it to version control (like Git), and only store it in a secure environment variable on your server.

### Live vs. Test Keys
Your dashboard has a toggle to switch between **Live Mode** and **Test Mode**.
* **Test Keys** are for development and testing using our [Sandbox Environment](./03-using-the-sandbox-environment.md). They do not process real money.
* **Live Keys** are for production and will process real payments from customers.

Make sure you are using the correct set of keys for your environment.