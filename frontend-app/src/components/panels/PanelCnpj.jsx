import React, { useState } from 'react';

function formatCnpj(val) {
    return val.replace(/\D/g, "")
        .replace(/^(\d{2})(\d)/, "$1.$2")
        .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/\.(\d{3})(\d)/, ".$1/$2")
        .replace(/(\d{4})(\d)/, "$1-$2")
        .slice(0, 18);
}

const PanelCnpj = ({ isActive }) => {
    const [cnpjInput, setCnpjInput] = useState('');
    const [error, setError] = useState('');
    const [result, setResult] = useState(null);
    const [isSearching, setIsSearching] = useState(false);

    const handleInputChange = (e) => {
        setCnpjInput(formatCnpj(e.target.value));
    };

    const handleSearch = async () => {
        const rawCnpj = cnpjInput.replace(/\D/g, "");
        setError('');
        setResult(null);

        if (rawCnpj.length < 14) {
            setError("Informe um CNPJ válido com 14 dígitos.");
            return;
        }

        setIsSearching(true);

        try {
            const resp = await fetch(`/cnpj/${rawCnpj}`);
            if (resp.status === 404) {
                setError("⚠️ CNPJ inexistente ou não encontrado na base de dados.");
            } else if (resp.ok) {
                const json = await resp.json();
                setResult(json);
            } else {
                setError("Erro ao consultar o CNPJ (status " + resp.status + "). Tente novamente.");
            }
        } catch (err) {
            setError("Erro de comunicação com o servidor: " + err.message);
        } finally {
            setIsSearching(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            handleSearch();
        }
    };

    return (
        <div className={`panel ${isActive ? 'active' : ''}`} id="panel-cnpj">
            <div style={{ display: 'flex', flexDirection: 'column', flex: 1, gap: '1.25rem', padding: '1.75rem', overflowY: 'auto' }}>
                <div className="cnpj-card">
                    <h3>🔍 Consulta de CNPJ</h3>
                    <div className="cnpj-search-row">
                        <div className="form-group">
                            <label htmlFor="cnpj-input">CNPJ</label>
                            <input
                                type="text"
                                id="cnpj-input"
                                placeholder="Digite o CNPJ a ser pesquisado"
                                maxLength="18"
                                value={cnpjInput}
                                onChange={handleInputChange}
                                onKeyDown={handleKeyDown}
                                disabled={isSearching}
                            />
                        </div>
                        <button
                            className="btn-search"
                            id="btn-pesquisar"
                            onClick={handleSearch}
                            disabled={isSearching}
                        >
                            {isSearching ? 'Pesquisando...' : 'Pesquisar'}
                        </button>
                    </div>
                    {error && <div id="cnpj-error" className="cnpj-error" style={{ display: 'block' }}>{error}</div>}
                </div>
                {result && (
                    <div id="cnpj-result-card" style={{ display: 'flex' }}>
                        <h3>📋 Resultado</h3>
                        <pre id="cnpj-result-pre">{JSON.stringify(result, null, 2)}</pre>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PanelCnpj;
