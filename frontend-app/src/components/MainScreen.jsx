import React, { useState, useEffect } from 'react';
import PanelChat from './panels/PanelChat';
import PanelProspeccao from './panels/PanelProspeccao';
import PanelProspeccaoContadores from './panels/PanelProspeccaoContadores';
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
    const [activePanel, setActivePanel] = useState('panel-chat-fillipo');
    const [activeTitle, setActiveTitle] = useState('Atendente Virtual - Fillipo');
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

    const getNavItemClass = (panelId, isSubitem = false) => {
        const baseClass = isSubitem ? 'nav-subitem' : 'nav-item';
        return `${baseClass} ${activePanel === panelId ? 'active' : ''}`;
    };

    const isAtendenteParentActive =
        activePanel === 'panel-chat-fillipo' || activePanel === 'panel-chat-luiggi';

    const isProspeccaoParentActive =
        activePanel === 'panel-prospeccao-fillipo' ||
        activePanel === 'panel-prospeccao-luiggi' ||
        activePanel === 'panel-prospeccao-contatos';

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

                    {/* Grupo de Atendente Virtual com Submenus */}
                    <div className="nav-group">
                        <div
                            className={`nav-item nav-parent ${isAtendenteParentActive ? 'parent-active' : ''}`}
                            id="nav-atendente"
                            data-panel="panel-chat-fillipo"
                            data-title="Atendente Virtual - Fillipo"
                            onClick={handleNavClick}
                        >
                            <span className="nav-icon">💬</span> Atendente Virtual
                        </div>
                        <div className="nav-subitems">
                            <div
                                className={getNavItemClass('panel-chat-fillipo', true)}
                                id="nav-chat-fillipo"
                                data-panel="panel-chat-fillipo"
                                data-title="Atendente Virtual - Fillipo"
                                onClick={handleNavClick}
                            >
                                <span className="nav-subicon">└</span> Fillipo
                            </div>
                            <div
                                className={getNavItemClass('panel-chat-luiggi', true)}
                                id="nav-chat-luiggi"
                                data-panel="panel-chat-luiggi"
                                data-title="Atendente Virtual - Luiggi"
                                onClick={handleNavClick}
                            >
                                <span className="nav-subicon">└</span> Luiggi
                            </div>
                        </div>
                    </div>

                    {/* Grupo de Prospecção com Submenus */}
                    <div className="nav-group">
                        <div
                            className={`nav-item nav-parent ${isProspeccaoParentActive ? 'parent-active' : ''}`}
                            id="nav-prospeccao"
                            data-panel="panel-prospeccao-fillipo"
                            data-title="Prospecção - Fillipo"
                            onClick={handleNavClick}
                        >
                            <span className="nav-icon">🎯</span> Prospecção
                        </div>
                        <div className="nav-subitems">
                            <div
                                className={getNavItemClass('panel-prospeccao-fillipo', true)}
                                id="nav-prospeccao-fillipo"
                                data-panel="panel-prospeccao-fillipo"
                                data-title="Prospecção - Fillipo"
                                onClick={handleNavClick}
                            >
                                <span className="nav-subicon">└</span> Fillipo
                            </div>
                            <div
                                className={getNavItemClass('panel-prospeccao-luiggi', true)}
                                id="nav-prospeccao-luiggi"
                                data-panel="panel-prospeccao-luiggi"
                                data-title="Prospecção - Luiggi"
                                onClick={handleNavClick}
                            >
                                <span className="nav-subicon">└</span> Luiggi
                            </div>
                            <div
                                className={getNavItemClass('panel-prospeccao-contatos', true)}
                                id="nav-prospeccao-contatos"
                                data-panel="panel-prospeccao-contatos"
                                data-title="Prospecção - Contatos em Geral"
                                onClick={handleNavClick}
                            >
                                <span className="nav-subicon">└</span> Contatos em Geral
                            </div>
                        </div>
                    </div>

                    <div
                        className={getNavItemClass('panel-cnpj')}
                        id="nav-cnpj"
                        data-panel="panel-cnpj"
                        data-title="Consultar CNPJ"
                        onClick={handleNavClick}
                    >
                        <span className="nav-icon">🔍</span> Consultar CNPJ
                    </div>

                    <div
                        className={getNavItemClass('panel-importar')}
                        id="nav-importar"
                        data-panel="panel-importar"
                        data-title="Importar Lista"
                        onClick={handleNavClick}
                    >
                        <span className="nav-icon">📂</span> Importar Lista
                    </div>

                    <div
                        className={getNavItemClass('panel-lote')}
                        id="nav-lote"
                        data-panel="panel-lote"
                        data-title="Pesquisar Lote"
                        onClick={handleNavClick}
                    >
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

                <PanelChat
                    isActive={activePanel === 'panel-chat-fillipo'}
                    assistantId="fillipo"
                    assistantName="Fillipo"
                    assistantRole="Especialista Comercial & B2B"
                    endpoint="/chat/stream"
                />
                <PanelChat
                    isActive={activePanel === 'panel-chat-luiggi'}
                    assistantId="luiggi"
                    assistantName="Luiggi"
                    assistantRole="Estrategista de Comércio Exterior"
                    endpoint="/chat/luiggi/stream"
                />
                <PanelProspeccaoContadores
                    isActive={activePanel === 'panel-prospeccao-fillipo'}
                    assistantId="fillipo"
                    assistantName="Fillipo"
                    segmentName="Contadores"
                    baseEndpoint="/prospecting-account"
                    icon="🎯"
                />
                <PanelProspeccaoContadores
                    isActive={activePanel === 'panel-prospeccao-luiggi'}
                    assistantId="luiggi"
                    assistantName="Luiggi"
                    segmentName="Comércio Exterior"
                    baseEndpoint="/prospecting-account/luiggi"
                    icon="🚢"
                />
                <PanelProspeccao isActive={activePanel === 'panel-prospeccao-contatos'} />
                <PanelCnpj isActive={activePanel === 'panel-cnpj'} />
                <PanelWip title="Importar Lista" icon="📂" isActive={activePanel === 'panel-importar'} />
                <PanelWip title="Pesquisar Lote" icon="📦" isActive={activePanel === 'panel-lote'} />
            </main>
        </div>
    );
}

export default MainScreen;
