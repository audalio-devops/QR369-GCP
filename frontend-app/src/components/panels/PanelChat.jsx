import React from 'react';

const PanelChat = () => {
    // A lógica do chat será migrada para cá futuramente
    return (
        <div style={{ display: 'flex', flexDirection: 'column', flex: 1, gap: '.75rem', padding: '1.25rem 1.75rem', overflow: 'hidden' }}>
            <div className="chat-toolbar">
                <button className="btn-secondary" id="btn-new-chat">+ Nova conversa</button>
            </div>
            <div id="chat-box" style={{ flex: 1 }}>
                {/* Mensagens do chat aparecerão aqui */}
            </div>
            <form className="chat-form" id="chat-form">
                <input id="chat-input" type="text" placeholder="Qual a sua dúvida?" autoComplete="off" required />
                <button type="submit" id="btn-send" className="btn-send">Enviar</button>
            </form>
        </div>
    );
};

export default PanelChat;
