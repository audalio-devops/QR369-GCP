import React, { useState, useEffect } from 'react';
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
    const [activePanel, setActivePanel] = useState('panel-chat');
    const [activeTitle, setActiveTitle] = useState('Atendente Virtual');
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth > 1023) {
                setIsSidebarOpen(false);
            }
        };

        const handleKeyDown = (event) => {
            if (event.key === 'Escape') {
                setIsSidebarOpen(false);
            }
        };

        const handleScroll = () => {
            if (isSidebarOpen) {
                setIsSidebarOpen(false);
            }
        };

        window.addEventListener('resize', handleResize);
        window.addEventListener('keydown', handleKeyDown);
        window.addEventListener('scroll', handleScroll, { passive: true });

        return () => {
            window.removeEventListener('resize', handleResize);
            window.removeEventListener('keydown', handleKeyDown);
            window.removeEventListener('scroll', handleScroll);
        };
    }, [isSidebarOpen]);

    const handleNavClick = (e) => {
        const navItem = e.currentTarget;
        const panelId = navItem.getAttribute('data-panel');
        const title = navItem.getAttribute('data-title');

        setActivePanel(panelId);
        setActiveTitle(title);
        setIsSidebarOpen(false);
    };

    const handleToggleMenu = () => {
        setIsSidebarOpen((prev) => !prev);
    };

    const handleCloseMenu = () => {
        setIsSidebarOpen(false);
    };

    const getNavItemClass = (panelId) => {
        return `nav-item ${activePanel === panelId ? 'active' : ''}`;
    };

    return (
        <div id="main-screen">
            <div
                className={`mobile-overlay ${isSidebarOpen ? 'visible' : ''}`}
                onClick={handleCloseMenu}
                role="presentation"
            />

            <aside id="sidebar" className={isSidebarOpen ? 'open' : ''}>
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

            <main id="content-area">
                <div className="content-header">
                    <button
                        className="mobile-menu-toggle"
                        type="button"
                        aria-label="Abrir menu"
                        aria-expanded={isSidebarOpen}
                        onClick={handleToggleMenu}
                    >
                        ☰
                    </button>
                    <div className="content-header-text">
                        <div className="mobile-app-title">QR<span>369</span>Tools</div>
                        <h2 id="content-title">{activeTitle}</h2>
                    </div>
                </div>

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
