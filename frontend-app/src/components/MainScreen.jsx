import React from 'react';

// Estes painéis serão transformados em seus próprios componentes mais tarde
const PanelChat = () => <div className="panel active" id="panel-chat">Conteúdo do Chat...</div>;
const PanelProspeccao = () => <div className="panel" id="panel-prospeccao">Conteúdo da Prospecção...</div>;
const PanelCnpj = () => <div className="panel" id="panel-cnpj">Conteúdo do CNPJ...</div>;
const PanelWip = ({ title, icon }) => (
    <div className="panel">
        <div className="panel-wip">
            <div className="wip-icon">{icon}</div>
            <div className="wip-title">{title}</div>
            <div className="wip-badge">Módulo em Desenvolvimento</div>
            <div className="wip-desc">Este módulo estará disponível em breve.</div>
        </div>
    </div>
);


function MainScreen() {
    // A lógica de navegação e estado será adicionada depois
    return (
        <div id="main-screen">
            {/* SIDEBAR */}
            <aside id="sidebar">
                <div className="sidebar-header">
                    <div className="sidebar-brand">QR<span>369</span> Tools</div>
                    <div className="sidebar-tagline">Sistema de Gestão</div>
                </div>
                <nav className="sidebar-nav">
                    <div className="nav-label">Módulos</div>
                    <div className="nav-item active" id="nav-chat" data-panel="panel-chat" data-title="Atendente Virtual">
                        <span className="nav-icon">💬</span> Atendente Virtual
                    </div>
                    <div className="nav-item" id="nav-prospeccao" data-panel="panel-prospeccao" data-title="Prospecção">
                        <span className="nav-icon">🎯</span> Prospecção
                    </div>
                    <div className="nav-item" id="nav-cnpj" data-panel="panel-cnpj" data-title="Consultar CNPJ">
                        <span className="nav-icon">🔍</span> Consultar CNPJ
                    </div>
                    <div className="nav-item" id="nav-importar" data-panel="panel-importar" data-title="Importar Lista">
                        <span className="nav-icon">📂</span> Importar Lista
                    </div>
                    <div className="nav-item" id="nav-lote" data-panel="panel-lote" data-title="Pesquisar Lote">
                        <span className="nav-icon">📦</span> Pesquisar Lote
                    </div>
                </nav>
                <div className="sidebar-footer">
                    <button className="btn-logout" id="btn-logout">⟵ Sair</button>
                </div>
            </aside>

            {/* CONTEÚDO PRINCIPAL */}
            <main id="content-area">
                <div className="content-header">
                    <h2 id="content-title">Atendente Virtual</h2>
                </div>

                {/* Os painéis serão renderizados aqui. A lógica de qual mostrar virá depois. */}
                <PanelChat />
                <PanelProspeccao />
                <PanelCnpj />
                <PanelWip title="Importar Lista" icon="📂" />
                <PanelWip title="Pesquisar Lote" icon="📦" />
            </main>
        </div>
    );
}

export default MainScreen;
