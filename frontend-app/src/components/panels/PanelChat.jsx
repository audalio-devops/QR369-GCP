import React, { useState, useEffect, useRef } from 'react';

function generateUUID() {
    if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
        return crypto.randomUUID();
    }
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        const v = c === "x" ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function escapeHtml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function renderMarkdown(md) {
    const blocks = [];
    let text = escapeHtml(md).replace(/```([\s\S]*?)```/g, (_, code) => {
        blocks.push("<pre><code>" + code.replace(/^\n+|\n+$/g, "") + "</code></pre>");
        return "\u0000" + (blocks.length - 1) + "\u0000";
    });
    text = text
        .replace(/`([^`]+)`/g, "<code>$1</code>")
        .replace(/^###\s+(.*)$/gm, "<h3>$1</h3>")
        .replace(/^##\s+(.*)$/gm, "<h2>$1</h2>")
        .replace(/^#\s+(.*)$/gm, "<h1>$1</h1>")
        .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
        .replace(/(^|[^*])\*([^*\n]+)\*/g, "$1<em>$2</em>")
        .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
            '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
    text = text
        .replace(/(?:^[-*]\s+.*(?:\n|$))+/gm, m =>
            "<ul>" + m.trim().split(/\n/).map(l => "<li>" + l.replace(/^[-*]\s+/, "") + "</li>").join("") + "</ul>")
        .replace(/(?:^\d+\.\s+.*(?:\n|$))+/gm, m =>
            "<ol>" + m.trim().split(/\n/).map(l => "<li>" + l.replace(/^\d+\.\s+/, "") + "</li>").join("") + "</ol>");
    text = text
        .split(/\n{2,}/)
        .map(part => /^\s*<(h\d|ul|ol|pre)/.test(part) ? part : "<p>" + part.replace(/\n/g, "<br>") + "</p>")
        .join("");
    return text.replace(/\u0000(\d+)\u0000/g, (_, i) => blocks[Number(i)]);
}

const PanelChat = ({ isActive }) => {
    const [messages, setMessages] = useState([]);
    const [inputText, setInputText] = useState('');
    const [isSending, setIsSending] = useState(false);
    const chatBoxRef = useRef(null);

    // Get or generate UUID for conversation session
    const getConversationId = () => {
        let id = localStorage.getItem("conversationId");
        if (!id) {
            id = generateUUID();
            localStorage.setItem("conversationId", id);
        }
        return id;
    };

    const startNewConversation = () => {
        localStorage.setItem("conversationId", generateUUID());
        setMessages([]);
        setInputText('');
    };

    // Auto-scroll to bottom of chat when messages change
    useEffect(() => {
        if (chatBoxRef.current) {
            chatBoxRef.current.scrollTop = chatBoxRef.current.scrollHeight;
        }
    }, [messages]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        const msgText = inputText.trim();
        if (!msgText || isSending) return;

        setInputText('');
        setIsSending(true);

        // Add user message
        const userMsg = { id: generateUUID(), sender: 'user', text: msgText };
        setMessages(prev => [...prev, userMsg]);

        // Add empty bot message to stream into
        const botMsgId = generateUUID();
        const botMsgPlaceholder = { id: botMsgId, sender: 'bot', text: '' };
        setMessages(prev => [...prev, botMsgPlaceholder]);

        let botText = "";
        let sseBuffer = "";

        const decodeSseEvent = (rawEvent) => {
            return rawEvent.split("\n")
                .filter(l => l.startsWith("data:"))
                .map(l => l.slice(5))
                .join("\n");
        };

        const consumeSse = (text, flush) => {
            sseBuffer += text.replace(/\r\n/g, "\n");
            let sep;
            while ((sep = sseBuffer.indexOf("\n\n")) !== -1) {
                botText += decodeSseEvent(sseBuffer.slice(0, sep));
                sseBuffer = sseBuffer.slice(sep + 2);
            }
            if (flush && sseBuffer.length) {
                botText += decodeSseEvent(sseBuffer);
                sseBuffer = "";
            }
        };

        try {
            const response = await fetch("/chat/stream", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-Conversation-Id": getConversationId()
                },
                body: JSON.stringify({ message: msgText })
            });

            if (!response.ok) {
                setMessages(prev => prev.map(m => m.id === botMsgId ? { ...m, text: `[Erro ${response.status}]` } : m));
                setIsSending(false);
                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let done = false;

            while (!done) {
                const { value, done: streamDone } = await reader.read();
                done = streamDone;
                if (value) {
                    consumeSse(decoder.decode(value, { stream: true }), false);
                    // Update state with ongoing bot response
                    setMessages(prev => prev.map(m => m.id === botMsgId ? { ...m, text: botText } : m));
                }
            }
            consumeSse("", true);
            setMessages(prev => prev.map(m => m.id === botMsgId ? { ...m, text: botText } : m));
        } catch (err) {
            setMessages(prev => prev.map(m => m.id === botMsgId ? { ...m, text: `[Falha de rede] ${err.message}` } : m));
        } finally {
            setIsSending(false);
        }
    };

    return (
        <div className={`panel ${isActive ? 'active' : ''}`} id="panel-chat">
            <div style={{ display: 'flex', flexDirection: 'column', flex: 1, gap: '.75rem', padding: '1.25rem 1.75rem', overflow: 'hidden' }}>
                <div className="chat-toolbar">
                    <button className="btn-secondary" id="btn-new-chat" onClick={startNewConversation}>
                        + Nova conversa
                    </button>
                </div>
                <div id="chat-box" ref={chatBoxRef} style={{ flex: 1 }}>
                    {messages.map((msg) => (
                        <div key={msg.id} className={`msg ${msg.sender}`}>
                            <div className="msg-label">{msg.sender === 'user' ? 'Você' : 'Atendente'}</div>
                            {msg.sender === 'user' ? (
                                <div className="msg-bubble">{msg.text}</div>
                            ) : (
                                <div className="msg-bubble" dangerouslySetInnerHTML={{ __html: renderMarkdown(msg.text) }} />
                            )}
                        </div>
                    ))}
                </div>
                <form className="chat-form" id="chat-form" onSubmit={handleSubmit}>
                    <input
                        id="chat-input"
                        type="text"
                        placeholder="Qual a sua dúvida?"
                        autoComplete="off"
                        required
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        disabled={isSending}
                    />
                    <button type="submit" id="btn-send" className="btn-send" disabled={isSending}>
                        {isSending ? 'Enviando...' : 'Enviar'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default PanelChat;
