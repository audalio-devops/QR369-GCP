import React, { useState, useEffect } from 'react';
import { ConfirmationDialog, useConfirmation, useToasts, ToastStack } from '../Toast';

const PanelProspeccao = ({ isActive }) => {
    const [contacts, setContacts] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const { toasts, pushToast, dismissToast } = useToasts();
    const { confirmation, confirm, confirmAction, cancelConfirmation } = useConfirmation();

    const loadProspectingData = async () => {
        setContacts([]);
        setIsLoading(true);
        setError('');

        try {
            const response = await fetch("/prospecting/read-file");

            if (response.status === 204) {
                setError("Nenhum contato encontrado na planilha.");
                return;
            }

            if (!response.ok) {
                throw new Error(`Erro ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            // Pula o cabeçalho (primeira linha)
            const contactsList = data.slice(1);

            if (!contactsList || contactsList.length === 0) {
                setError("Nenhum contato encontrado na planilha.");
                return;
            }

            setContacts(contactsList);
        } catch (err) {
            console.error("Erro ao carregar dados da prospecção:", err);
            setError(`Falha ao carregar contatos. Verifique se o serviço de prospecção está rodando. Detalhes: ${err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (isActive) {
            loadProspectingData();
        }
    }, [isActive]);

    const handleIniciarProspeccao = async () => {
        const confirmed = await confirm({
            type: 'info',
            title: 'Iniciar prospecção',
            text: 'Deseja mesmo iniciar a prospecção?'
        });
        if (confirmed) {
            const now = new Date();
            pushToast({
                type: 'success',
                title: 'Prospecção iniciada',
                text: `Iniciada às ${now.toLocaleDateString('pt-BR')} ${now.toLocaleTimeString('pt-BR')}`,
            });
        } else {
            pushToast({
                type: 'info',
                title: 'Prospecção cancelada',
                text: 'Nenhuma ação foi executada.',
            });
        }
    };

    return (
        <div className={`panel ${isActive ? 'active' : ''}`} id="panel-prospeccao">
            <ToastStack toasts={toasts} onDismiss={dismissToast} />
            <ConfirmationDialog confirmation={confirmation} onConfirm={confirmAction} onCancel={cancelConfirmation} />
            <div className="prospeccao-card">
                <div className="prospeccao-header">
                    <h3>🎯 Lista de Contatos</h3>
                    <button className="btn-prospect" id="btn-iniciar-prospeccao" onClick={handleIniciarProspeccao}>
                        Iniciar Prospecção
                    </button>
                </div>
                {error && <div id="prospeccao-error" className="prospeccao-error" style={{ display: 'block' }}>{error}</div>}
                <div className="prospeccao-table-container">
                    <table id="prospeccao-table">
                        <thead>
                            <tr>
                                <th>Contato</th>
                                <th>Nro do Contato</th>
                                <th>Data Contactado</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="prospeccao-table-body">
                            {contacts.map((row, index) => {
                                const [contato, nroContato, dataContactado, status] = row;
                                return (
                                    <tr key={index}>
                                        <td>{contato || ''}</td>
                                        <td>{nroContato || ''}</td>
                                        <td>{dataContactado || ''}</td>
                                        <td>{status || ''}</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                    {isLoading && (
                        <div id="prospeccao-loading" className="prospeccao-loading">
                            Carregando contatos...
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PanelProspeccao;
