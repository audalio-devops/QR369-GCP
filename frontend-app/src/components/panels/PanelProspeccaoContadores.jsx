import React, { useState, useEffect } from 'react';
import { ConfirmationDialog, useConfirmation, useToasts, ToastStack } from '../Toast';

// Quantidade máxima de registros de auditoria exibidos na tabela
const AUDIT_LOGS_LIMIT = 30;

const PanelProspeccaoContadores = ({
    isActive,
    assistantId = 'fillipo',
    assistantName = 'Fillipo',
    segmentName = 'Contadores',
    baseEndpoint = '/prospecting-account',
    icon = '🎯'
}) => {
    const [auditLogs, setAuditLogs] = useState([]);
    const [isLoadingLogs, setIsLoadingLogs] = useState(false);
    const [statusInfo, setStatusInfo] = useState({
        checkedAt: null,
        isRunning: null,
        lastError: null,
        message: ''
    });
    const { toasts, pushToast, dismissToast } = useToasts();
    const { confirmation, confirm, confirmAction, cancelConfirmation } = useConfirmation();

    const fetchAuditLogs = async () => {
        setIsLoadingLogs(true);
        try {
            const response = await fetch(`${baseEndpoint}/audit?limit=${AUDIT_LOGS_LIMIT}`);
            if (response.ok) {
                const data = await response.json();
                setAuditLogs(data.slice(0, AUDIT_LOGS_LIMIT));
            } else {
                console.error('Erro ao buscar logs de auditoria:', response.statusText);
            }
        } catch (err) {
            console.error('Falha ao conectar com o serviço de auditoria:', err);
        } finally {
            setIsLoadingLogs(false);
        }
    };

    const handleVerificarStatus = async (isSilent = false) => {
        const silent = isSilent === true;
        try {
            const response = await fetch(`${baseEndpoint}/status`);
            const now = new Date();
            const formattedTime = now.toLocaleDateString('pt-BR') + ' ' + now.toLocaleTimeString('pt-BR');

            if (response.ok) {
                const data = await response.json();
                setStatusInfo({
                    checkedAt: formattedTime,
                    isRunning: data.running,
                    lastError: data.lastError || null,
                    message: data.running ? 'Prospecção em andamento.' : (data.lastError || 'Prospecção parada.')
                });

                if (data.lastError) {
                    pushToast({
                        type: 'error',
                        title: 'Erro na Prospecção',
                        text: data.lastError
                    });
                } else if (!silent) {
                    pushToast({
                        type: 'info',
                        title: 'Status da Prospecção',
                        text: data.running ? 'Prospecção está em andamento.' : 'Prospecção está parada.'
                    });
                }
            } else {
                const errorMsg = `Serviço respondeu com erro: ${response.status} ${response.statusText}`;
                setStatusInfo({
                    checkedAt: formattedTime,
                    isRunning: false,
                    lastError: errorMsg,
                    message: errorMsg
                });
                if (!silent) {
                    pushToast({ type: 'error', title: 'Falha na verificação', text: errorMsg });
                }
            }
        } catch (err) {
            const now = new Date();
            const formattedTime = now.toLocaleDateString('pt-BR') + ' ' + now.toLocaleTimeString('pt-BR');
            const errorMsg = `Não foi possível conectar ao serviço: ${err.message}`;
            setStatusInfo({
                checkedAt: formattedTime,
                isRunning: false,
                lastError: errorMsg,
                message: errorMsg
            });
            if (!silent) {
                pushToast({ type: 'error', title: 'Erro de Conexão', text: errorMsg });
            }
        } finally {
            fetchAuditLogs();
        }
    };

    const handleIniciarProspeccao = async () => {
        const confirmed = await confirm({
            type: 'info',
            title: `Iniciar prospecção - ${assistantName}`,
            text: `Deseja iniciar a prospecção de ${segmentName.toLowerCase()} (${assistantName})?`
        });
        if (!confirmed) return;

        try {
            const response = await fetch(baseEndpoint, { method: 'POST' });
            const messageText = await response.text();

            if (response.status === 202) {
                pushToast({ type: 'success', title: 'Prospecção iniciada', text: messageText });
            } else if (response.status === 409) {
                pushToast({ type: 'warning', title: 'Prospecção já em andamento', text: messageText });
            } else {
                pushToast({ type: 'error', title: `Erro ${response.status}`, text: messageText });
            }
        } catch (err) {
            pushToast({ type: 'error', title: 'Falha ao iniciar prospecção', text: err.message });
        } finally {
            handleVerificarStatus(true);
        }
    };

    const handlePararProspeccao = async () => {
        const confirmed = await confirm({
            type: 'warning',
            title: `Parar prospecção - ${assistantName}`,
            text: `Deseja solicitar a parada da prospecção de ${segmentName.toLowerCase()} (${assistantName})?`,
            confirmLabel: 'Solicitar parada'
        });
        if (!confirmed) return;

        try {
            const response = await fetch(baseEndpoint, { method: 'DELETE' });
            const messageText = await response.text();

            if (response.ok) {
                pushToast({ type: 'warning', title: 'Parada solicitada', text: messageText });
            } else {
                pushToast({ type: 'error', title: `Erro ${response.status}`, text: messageText });
            }
        } catch (err) {
            pushToast({ type: 'error', title: 'Falha ao enviar sinal de parada', text: err.message });
        } finally {
            handleVerificarStatus(true);
        }
    };

    useEffect(() => {
        if (isActive) {
            handleVerificarStatus(true);
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
        <div className={`panel ${isActive ? 'active' : ''}`} id={`panel-prospeccao-${assistantId}`}>
            <ToastStack toasts={toasts} onDismiss={dismissToast} />
            <ConfirmationDialog confirmation={confirmation} onConfirm={confirmAction} onCancel={cancelConfirmation} />
            <div className="prospeccao-card">
                <div className="prospeccao-header">
                    <div>
                        <h3>{icon} Prospecção — {assistantName}</h3>
                        <p className="prospeccao-subtitle">Controle de execução e histórico de eventos para {segmentName}</p>
                    </div>

                    <div className="prospeccao-actions">
                        <button
                            className="btn-prospect btn-iniciar"
                            id={`btn-iniciar-prospeccao-${assistantId}`}
                            onClick={handleIniciarProspeccao}
                        >
                            ▶ Iniciar Prospecção
                        </button>
                        <button
                            className="btn-prospect btn-parar"
                            id={`btn-parar-prospeccao-${assistantId}`}
                            onClick={handlePararProspeccao}
                        >
                            ⏹ Parar Prospecção
                        </button>
                        <button
                            className="btn-prospect btn-status"
                            id={`btn-status-prospeccao-${assistantId}`}
                            onClick={() => handleVerificarStatus(false)}
                        >
                            🔍 Verificar Status
                        </button>
                    </div>
                </div>

                {/* Banner de Erro Crítico na tela (ex: Instância Z-API desconectada) */}
                {statusInfo.lastError && (
                    <div className="prospeccao-error-box" id={`prospeccao-status-error-${assistantId}`} style={{
                        backgroundColor: 'rgba(239, 68, 68, 0.15)',
                        border: '1px solid rgba(239, 68, 68, 0.4)',
                        borderRadius: '8px',
                        padding: '12px 16px',
                        marginBottom: '16px',
                        color: '#f87171',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '10px',
                        fontWeight: 600
                    }}>
                        <span style={{ fontSize: '20px' }}>⚠️</span>
                        <span>{statusInfo.lastError}</span>
                    </div>
                )}

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

                {/* Tabela de Logs de Auditoria */}
                <div className="prospeccao-audit-section">
                    <div className="audit-section-header">
                        <h4>📋 Registros de Auditoria Mais Recentes (Máx. {AUDIT_LOGS_LIMIT})</h4>
                        <button className="btn-refresh-audit" onClick={fetchAuditLogs} disabled={isLoadingLogs}>
                            🔄 {isLoadingLogs ? 'Atualizando...' : 'Atualizar Logs'}
                        </button>
                    </div>

                    <div className="prospeccao-table-container audit-table-container">
                        <table id={`audit-table-${assistantId}`}>
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
