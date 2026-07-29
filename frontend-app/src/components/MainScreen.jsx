import React, { useState } from 'react';
import PanelChat from './panels/PanelChat';
import PanelProspeccao from './panels/PanelProspeccao';
import PanelCnpj from './panels/PanelCnpj';
const PanelWip = ({ title, icon, isActive }) => (
    <div className={`panel ${isActive ? 'active' : ''}`}>
        <div className="panel-wip">
            <div className="wip-icon">{icon}</div>
            <div className="wip-title">{title}</div>
            <div className="wip-badge">Módulo em Desenvolvimento</div>
            <div className="wip-desc">Este módulo estará disponível em breve.</div>
        </div>
    </div>
);


function MainScreen({ onLogout }) {
    // Estado para controlar o painel ativo e o título
    const [activePanel, setActivePanel] = useState('panel-chat');
    const [activeTitle, setActiveTitle] = useState('Atendente Virtual');

    // Função para lidar com o clique na navegação
    const handleNavClick = (e) => {
        const navItem = e.currentTarget;
        const panelId = navItem.getAttribute('data-panel');
        const title = navItem.getAttribute('data-title');

        setActivePanel(panelId);
        setActiveTitle(title);
    };

    // Helper para gerar a classe do item de navegação
    const getNavItemClass = (panelId) => {
        return `nav-item ${activePanel === panelId ? 'active' : ''}`;
    };

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
                    <div className={getNavItemClass('panel-chat')} id="nav-chat" data-panel="panel-chat" data-title="Atendente Virtual" onClick={handleNavClick}>
                        <span className="nav-icon">💬</span> Atendente Virtual
                    </div>
                    <div className={getNavItemClass('panel-prospeccao')} id="nav-prospeccao" data-panel="panel-prospeccao" data-title="Prospecção" onClick={handleNavClick}>
                        <span className="nav-icon">🎯</span> Prospecção
                    </div>
                    <div className={getNavItemClass('panel-cnpj')} id="nav-cnpj" data-panel="panel-cnpj" data-title="Consultar CNPJ" onClick={handleNavClick}>
                        <span className="nav-icon">🔍</span> Consultar CNPJ
                    </div>
                    <div className={getNavItemClass('panel-importar')} id="nav-importar" data-panel="panel-importar" data-title="Importar Lista" onClick={handleNavClick}>
                        <span className="nav-icon">📂</span> Importar Lista
                    </div>
                    <div className={getNavItemClass('panel-lote')} id="nav-lote" data-panel="panel-lote" data-title="Pesquisar Lote" onClick={handleNavClick}>
                        <span className="nav-icon">📦</span> Pesquisar Lote
                    </div>
                </nav>
                <div className="sidebar-footer">
                    <button className="btn-logout" id="btn-logout" onClick={onLogout}>
                        ⟵ Sair
                    </button>
                </div>
            </aside>

            {/* CONTEÚDO PRINCIPAL */}
            <main id="content-area">
                <div className="content-header">
                    <h2 id="content-title">{activeTitle}</h2>
                </div>

                {/* Os painéis são renderizados condicionalmente com base no estado 'activePanel' */}
                <PanelChat isActive={activePanel === 'panel-chat'} />
                <PanelProspeccao isActive={activePanel === 'panel-prospeccao'} />
                <PanelCnpj isActive={activePanel === 'panel-cnpj'} />
                <PanelWip title="Importar Lista" icon="📂" isActive={activePanel === 'panel-importar'} />
                <PanelWip title="Pesquisar Lote" icon="📦" isActive={activePanel === 'panel-lote'} />
            </main>
        </div>
    );
}

export default MainScreen;
