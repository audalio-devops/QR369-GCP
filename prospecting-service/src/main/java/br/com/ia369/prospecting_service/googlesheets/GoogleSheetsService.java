package br.com.ia369.prospecting_service.googlesheets;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "OCI Java Sheets Integration";
    // https://docs.google.com/spreadsheets/d/1OBNHijTLOqMOPJVSjutVSDJqBfGDF8jxbEOPEHJEp00/edit?usp=sharing
    private static final String SPREADSHEET_ID = "1OBNHijTLOqMOPJVSjutVSDJqBfGDF8jxbEOPEHJEp00";
    // Caminho do arquivo de credenciais dentro do classpath
    private static final String CREDENTIALS_FILE_PATH = "credentials/qr369tools.json";

    public List<List<Object>> readSheet() throws GeneralSecurityException, IOException {
        Sheets sheetsService = getSheetsService();

        // 1. LER OS DADOS DA PLANILHA (Ex: Intervalo da aba 'Página1' colunas A até C)
        String range = "Página1!A:C";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> rows = response.getValues();
        if (rows == null || rows.isEmpty()) {
            System.out.println("Nenhum dado encontrado na planilha.");
            return null;
        }
        return rows;
    }

    private static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        // Carrega as credenciais da conta de serviço a partir do classpath
        InputStream credentialsStream = GoogleSheetsService.class.getClassLoader().getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (credentialsStream == null) {
            throw new IOException("Arquivo de credenciais não encontrado no classpath: " + CREDENTIALS_FILE_PATH);
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(List.of(SheetsScopes.SPREADSHEETS));

        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        return new Sheets.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static void atualizarCelula(Sheets service, String spreadsheetId, String range, String novoValor)
            throws IOException {
        ValueRange body = new ValueRange().setValues(List.of(List.of(novoValor)));

        service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED") // Permite que o Google interprete o tipo de dado (texto, número,
                                                     // etc.)
                .execute();

        System.out.println("-> Célula " + range + " atualizada com sucesso para: " + novoValor);
    }
}
