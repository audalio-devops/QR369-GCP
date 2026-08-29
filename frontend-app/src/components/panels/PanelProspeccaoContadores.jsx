import React, { useState, useEffect } from 'react';

const PanelProspeccaoContadores = ({ isActive }) => {
    const [auditLogs, setAuditLogs] = useState([]);
    const [isLoadingLogs, setIsLoadingLogs] = useState(false);
    const [statusInfo, setStatusInfo] = useState({
        checkedAt: null,
        isRunning: null,
        message: ''
    });
    const [actionMessage, setActionMessage] = useState({ text: '', type: '' });

    const fetchAuditLogs = async () => {
        setIsLoadingLogs(true);
        try {
            const response = await fetch('/prospecting-account/audit');
            if (response.ok) {
                const data = await response.json();
                setAuditLogs(data);
            } else {
                console.error('Erro ao buscar logs de auditoria:', response.statusText);
            }
        } catch (err) {
            console.error('Falha ao conectar com o serviço de auditoria:', err);
        } finally {
            setIsLoadingLogs(false);
        }
    };

    const handleVerificarStatus = async () => {
        setActionMessage({ text: '', type: '' });
        try {
            const response = await fetch('/prospecting-account/status');
            const now = new Date();
            const formattedTime = now.toLocaleDateString('pt-BR') + ' ' + now.toLocaleTimeString('pt-BR');

            if (response.ok) {
                const data = await response.json();
                setStatusInfo({
                    checkedAt: formattedTime,
                    isRunning: data.running,
                    message: data.running ? 'Prospecção em andamento.' : 'Prospecção parada.'
                });
                setActionMessage({
                    text: `Verificação concluída às ${formattedTime}. Status: ${data.running ? 'EM EXECUÇÃO' : 'PARADO'}`,
                    type: 'info'
                });
            } else {
                throw new Error(`Status HTTP ${response.status}`);
            }
        } catch (err) {
            setStatusInfo(prev => ({
                ...prev,
                message: 'Erro ao obter status do serviço.'
            }));
            setActionMessage({
                text: `Falha ao verificar status: ${err.message}`,
                type: 'error'
            });
        } finally {
            fetchAuditLogs();
        }
    };

    const handleIniciarProspeccao = async () => {
        if (!window.confirm('Deseja iniciar a prospecção de contadores?')) return;

        setActionMessage({ text: '', type: '' });
        try {
            const response = await fetch('/prospecting-account', { method: 'POST' });
            const messageText = await response.text();

            if (response.status === 202) {
                setActionMessage({ text: `✅ ${messageText}`, type: 'success' });
            } else if (response.status === 409) {
                setActionMessage({ text: `⚠️ ${messageText}`, type: 'warning' });
            } else {
                setActionMessage({ text: `❌ Erro ${response.status}: ${messageText}`, type: 'error' });
            }
        } catch (err) {
            setActionMessage({ text: `❌ Falha ao conectar ao serviço: ${err.message}`, type: 'error' });
        } finally {
            handleVerificarStatus();
        }
    };

    const handlePararProspeccao = async () => {
        if (!window.confirm('Deseja solicitar a parada da prospecção de contadores?')) return;

        setActionMessage({ text: '', type: '' });
        try {
            const response = await fetch('/prospecting-account', { method: 'DELETE' });
            const messageText = await response.text();

            if (response.ok) {
                setActionMessage({ text: `🛑 ${messageText}`, type: 'warning' });
            } else {
                setActionMessage({ text: `❌ Erro ${response.status}: ${messageText}`, type: 'error' });
            }
        } catch (err) {
            setActionMessage({ text: `❌ Falha ao enviar sinal de parada: ${err.message}`, type: 'error' });
        } finally {
            handleVerificarStatus();
        }
    };

    useEffect(() => {
        if (isActive) {
            handleVerificarStatus();
        }
    }, [isActive]);

    const formatDataEvento = (isoString) => {
        if (!isoString) return '-';
        try {
            const date = new Date(isoString);
            return date.toLocaleDateString('pt-BR') + ' ' + date.toLocaleTimeString('pt-BR');
        } catch {
            return isoString;
        }
    };

    const getStatusBadgeClass = (status) => {
        if (!status) return 'badge-default';
        const s = status.toLowerCase();
        if (s.includes('iniciado')) return 'badge-iniciado';
        if (s.includes('funcionando') || s.includes('ok')) return 'badge-funcionando';
        if (s.includes('finalizado')) return 'badge-finalizado';
        if (s.includes('erro') || s.includes('error')) return 'badge-erro';
        return 'badge-default';
    };

    return (
        <div className={`panel ${isActive ? 'active' : ''}`} id="panel-prospeccao-contadores">
            <div className="prospeccao-card">
                <div className="prospeccao-header">
                    <div>
                        <h3>🎯 Prospecção de Contadores</h3>
                        <p className="prospeccao-subtitle">Controle de execução e histórico de eventos em tempo real</p>
                    </div>

                    <div className="prospeccao-actions">
                        <button
                            className="btn-prospect btn-iniciar"
                            id="btn-iniciar-prospeccao-contadores"
                            onClick={handleIniciarProspeccao}
                        >
                            ▶ Iniciar Prospecção
                        </button>
                        <button
                            className="btn-prospect btn-parar"
                            id="btn-parar-prospeccao-contadores"
                            onClick={handlePararProspeccao}
                        >
                            ⏹ Parar Prospecção
                        </button>
                        <button
                            className="btn-prospect btn-status"
                            id="btn-status-prospeccao-contadores"
                            onClick={handleVerificarStatus}
                        >
                            🔍 Verificar Status
                        </button>
                    </div>
                </div>

                {/* Bloco com o resultado da última verificação */}
                <div className="status-monitor-box">
                    <div className="status-monitor-item">
                        <span className="status-monitor-label">Horário da Última Verificação:</span>
                        <span className="status-monitor-value">
                            {statusInfo.checkedAt ? statusInfo.checkedAt : 'Ainda não verificado'}
                        </span>
                    </div>
                    <div className="status-monitor-item">
                        <span className="status-monitor-label">Status da Execução:</span>
                        <span className={`status-pill ${statusInfo.isRunning ? 'running' : 'stopped'}`}>
                            {statusInfo.isRunning === null
                                ? 'Desconhecido'
                                : statusInfo.isRunning
                                    ? '● Em Execução'
                                    : '○ Parado'}
                        </span>
                    </div>
                </div>

                {actionMessage.text && (
                    <div className={`prospeccao-alert alert-${actionMessage.type}`}>
                        {actionMessage.text}
                    </div>
                )}

                {/* Tabela de Logs de Auditoria */}
                <div className="prospeccao-audit-section">
                    <div className="audit-section-header">
                        <h4>📋 Registros de Auditoria Mais Recentes (Máx. 10)</h4>
                        <button className="btn-refresh-audit" onClick={fetchAuditLogs} disabled={isLoadingLogs}>
                            🔄 {isLoadingLogs ? 'Atualizando...' : 'Atualizar Logs'}
                        </button>
                    </div>

                    <div className="prospeccao-table-container">
                        <table id="audit-table">
                            <thead>
                                <tr>
                                    <th>Data / Hora</th>
                                    <th>CNPJ</th>
                                    <th>Status</th>
                                    <th>Log / Mensagem</th>
                                </tr>
                            </thead>
                            <tbody>
                                {auditLogs.length === 0 ? (
                                    <tr>
                                        <td colSpan="4" className="table-empty">
                                            {isLoadingLogs ? 'Carregando logs...' : 'Nenhum registro de auditoria encontrado.'}
                                        </td>
                                    </tr>
                                ) : (
                                    auditLogs.map((log) => (
                                        <tr key={log.id}>
                                            <td className="cell-nowrap">{formatDataEvento(log.dataEvento)}</td>
                                            <td className="cell-nowrap">{log.cnpj || '-'}</td>
                                            <td>
                                                <span className={`audit-badge ${getStatusBadgeClass(log.status)}`}>
                                                    {log.status}
                                                </span>
                                            </td>
                                            <td>{log.log || '-'}</td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PanelProspeccaoContadores;
