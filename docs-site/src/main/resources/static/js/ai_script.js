function aiController() {
    return {
        userInput: '',
        isLoading: false,
        // The chatHistory will store the conversation objects
        chatHistory: [], // e.g., { role: 'user' | 'ai', content: '...', sources: [] }

        // Initialize the controller and add the first welcome message
        initAi() {
            this.chatHistory.push({
                role: 'ai',
                content: '<p>Hello! Ask me anything about our documentation.</p>',
                sources: []
            });
        },

        // Handles sending the user's query to the backend
        async submitQuery() {
            const query = this.userInput.trim();
            if (!query || this.isLoading) return;

            // Add user message to the chat display
            this.chatHistory.push({role: 'user', content: query});
            this.userInput = '';
            this.isLoading = true;
            this.$nextTick(() => this.scrollToBottom());

            try {
                const response = await fetch('/api/ai/ask', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
                    body: JSON.stringify({query: query})
                });

                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({message: 'An unknown error occurred.'}));
                    throw new Error(errorData.message || `Server responded with status: ${response.status}`);
                }

                const data = await response.json();
                // Add the AI's response and sources to the chat display
                this.chatHistory.push({
                    role: 'ai',
                    content: this.formatMarkdown(data.answer),
                    sources: data.sources || []
                });

            } catch (error) {
                console.error('Error fetching AI response:', error);
                this.chatHistory.push({
                    role: 'ai',
                    content: `<p class="text-red-500">Sorry, I encountered an error: ${error.message}</p>`,
                    sources: []
                });
            } finally {
                this.isLoading = false;
                this.$nextTick(() => this.scrollToBottom());
            }
        },

        // A simple formatter to turn newlines in the AI response into paragraphs
        formatMarkdown(text) {
            if (!text) return '';
            // Escape HTML to prevent XSS, then format
            const escapedText = text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
            return escapedText.split('\n').map(p => `<p>${p}</p>`).join('');
        },

        // Clears the chat history and resets to the welcome message
        clearHistory() {
            this.chatHistory = [{
                role: 'ai',
                content: '<p>History cleared. How can I help you now?</p>',
                sources: []
            }];
        },

        // Utility to auto-scroll the chat window to the latest message
        scrollToBottom() {
            const chatContainer = this.$refs.chatContainer;
            if (chatContainer) {
                chatContainer.scrollTop = chatContainer.scrollHeight;
            }
        }
    }
}