package org.mabera.jasper.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Main {

    static String JASPER_REPORT_URL_REST = "http://localhost:8081/jasperserver-pro/rest_v2/reportExecutions";
    static String JASPER_REPORT_RESOURCE_URL_REST = "http://localhost:8081/jasperserver-pro/rest_v2/reportExecutions/";
    static String REPORT_OUTPUT_RESOURCE_URL = "";

    public static void jasperEmbeddingWithHttpAPI(OkHttpClient client,Map<String,String> parameters) throws URISyntaxException, IOException {

        String reportServerURL = "http://localhost:8081";

        String username = "jasperadmin";
        String password = "jasperadmin";

        String ParentFolderUri = "public/MyReports";
        String reportName = "FarmerLogReport";

        Map<String, String> reportParams = new HashMap<>();
        reportParams.put("byEmail", "mabera@gmail.com");

        System.out.println("Report Params"+reportParams);
        System.out.println("Parameters"+parameters);

        String reportURL = reportServerURL
                + "/jasperserver-pro/flow.html?_flowId=viewReportFlow&_flowId=viewReportFlow&ParentFolderUri=%2F"
                + ParentFolderUri + "&reportUnit=%2F" + ParentFolderUri +
                "%2F" + reportName + "&standAlone=true" +
                "&j_username" + "=" + username + "&j_password=" + password + "&"
                + concatReportParamsForURL(parameters) + "output=pdf";
        // http://localhost:8081/jasperserver-pro/flow.html?_flowId=viewReportFlow&_flowId=viewReportFlow&ParentFolderUri=%2Fpublic%2FMyReports&reportUnit=%2Fpublic%2FMyReports%2FFarmerSystemjrxml&standAlone=true
        System.out.println(reportURL);
        URI uriFromString = new URI(reportURL);

        openHtmlFileinBrowser(reportURL,client);

        // java.awt.Desktop.getDesktop().browse(uriFromString);

    }

    private static void openHtmlFileinBrowser(String url, OkHttpClient client) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            openInBrowser(url);
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to open URL in the default browser
    private static void openInBrowser(String url) {
        try {
            Runtime.getRuntime().exec("xdg-open " + url);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String concatReportParamsForURL(Map<String, String> reportParams) {
        StringBuilder concatenatedReportParam = new StringBuilder();

        for (Map.Entry<String, String> reportParam : reportParams.entrySet()) {
            concatenatedReportParam.append(reportParam.getKey()).append("=").append(reportParam.getValue()).append("&");
        }

        return concatenatedReportParam.toString();
    }

    public static Map<String, Object> convertJsonToMap(String jsonString) throws IOException {

        System.out.println("The json string is"+jsonString);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, Map.class);
    }

    private static void openHtmlFile(String outputFile) throws IOException {
        File file = new File(outputFile);
        if (file.exists()) {
            URI uri = file.toURI();
            java.awt.Desktop.getDesktop().browse(uri);
        } else {
            throw new FileNotFoundException("File not found: " + outputFile);
        }
    }

    private static CompletableFuture<String> sendRequest(OkHttpClient client, Request request) {
        CompletableFuture<String> future = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response reportExeResponse) throws IOException {
                if (reportExeResponse.isSuccessful()) {
                    // System.out.println("Response data is " + reportExeResponse.body().string());
                    Map<String, Object> reportExeResponseData = convertJsonToMap(reportExeResponse.body().string());
                    List<Map<String, String>> exports = (List<Map<String, String>>) reportExeResponseData
                            .get("exports");
                    String reportExecutionStatus = exports.get(0).get("status");
                    future.complete(reportExecutionStatus);
                } else {
                    future.completeExceptionally(
                            new RuntimeException("Request failed with status code: " + reportExeResponse.code()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private static void downloadTheReportOutput(OkHttpClient client, String cookies, String outputResourceURL,
            String authHeader) {

        String username = System.getProperty("user.name");
        String outputFile = String.format("/home/%s/Documents/farmer_report.html", username);
        String outputURL = String.format("file:///home/%s/Documents/farmer_report.html",username);

        Request outputResourceRequest = new Request.Builder()
                .url(outputResourceURL)
                .addHeader("content-type", "application/xml")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", authHeader)
                .addHeader("Cookie", cookies)
                .build();

        try (Response response = client.newCall(outputResourceRequest).execute()) {

            System.out.println("\nDownload Response: " + response.code());
            // System.out.println(response.body().string());

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(response.body().bytes());
                openHtmlFileinBrowser(outputURL,client);
            } catch (Exception e) {
                System.out.println("The exception is " + e.getMessage());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void checkStatusAndDownloadReport(OkHttpClient client, Request request, String cookies,
            String authHeader) {
        // Send initial request to check status
        sendRequest(client, request).thenAccept(reportExecutionStatus -> {

            // Check if status is "ready"
            if ("ready".equals(reportExecutionStatus)) {
                // if ready get the report
                String reportResourceResponse;
                try (Response response = client.newCall(request).execute()) {
                    reportResourceResponse = (response.body().string());

                    System.out.println("\nStatus Ready Response: " + response.code());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(reportResourceResponse);

                // download the report
                System.out.println("\nDownloading...");
                downloadTheReportOutput(client, cookies, REPORT_OUTPUT_RESOURCE_URL, authHeader);
                System.out.println("Downloaded");
                System.exit(0);

            } else {
                // If status is not "ready", schedule another check
                // Schedule the next check after a delay of 1 second
                CompletableFuture.delayedExecutor(1, java.util.concurrent.TimeUnit.SECONDS)
                        .execute(() -> checkStatusAndDownloadReport(client, request, cookies, authHeader));
            }
        });
    }

    public static void jasperEmbeddingWithRESTAPI(OkHttpClient okHttpClient, Map<String, String> parameters) throws IOException {

        // username:password combination
        String credentials = "jasperadmin" + ":" + "jasperadmin";

        // Basic Auth
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        String authHeader = "Basic " + encodedCredentials;

        // decode the input map
        String key ="";
        String value="";

        for(Map.Entry<String, String> entry:parameters.entrySet()){
             key = entry.getKey();
             value = entry.getValue();
        }

        StringBuilder cookies = new StringBuilder();

        String reportExecutionXmlBody = "<reportExecutionRequest>\n" +
                "    <reportUnitUri>/reports/interactive/FarmerLogReport</reportUnitUri>\n" +
                "    <reportContainerWidth>900</reportContainerWidth>\n" +
                "    <async>true</async>\n" +
                "    <freshData>false</freshData>\n" +
                "    <saveDataSnapshot>false</saveDataSnapshot>\n" +
                "    <outputFormat>html</outputFormat>\n" +
                "    <interactive>true</interactive>\n" +
                "    <ignorePagination>false</ignorePagination>\n" +
                "    <pages>1-5</pages>\n" +
                "    <parameters>\n" +
                "    <reportParameter name="+key+">\n" +
                "        <value>"+value+"</value>\n" +
                "    </reportParameter>\n" +
                "</parameters>"+
                "</reportExecutionRequest>";

        // SEND EXECUTION REQUEST

        RequestBody reportExeRequestBody = RequestBody.create(reportExecutionXmlBody,
                MediaType.parse("application/xml"));

        Request jasperReportExeRequest = new Request.Builder()
                .url(JASPER_REPORT_URL_REST)
                .addHeader("content-type", "application/xml")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", authHeader)
                .post(reportExeRequestBody)
                .build();

        String reportExeResponseStr = """
                """; // verbatim string

        try (Response response = okHttpClient.newCall(jasperReportExeRequest).execute()) {
            reportExeResponseStr = reportExeResponseStr + (response.body().string());

            System.out.println(jasperReportExeRequest);
            System.out.println(response.headers());
            List<Cookie> cookiesObj = Cookie.parseAll(jasperReportExeRequest.url(), response.headers());

            for (Cookie cookieObj : cookiesObj) {
                System.out.println("The cookie object is " + cookieObj);
                cookies.append(cookieObj.toString()).append("; ");
            }

            // Remove the trailing "; "
            if (!cookies.isEmpty()) {
                cookies.setLength(cookies.length() - 2);
            } else {
                System.out.println("we have no cookies");
            }

            System.out.println("Cookie: " + cookies);
            System.out.println("\nReport Exe Response: " + response.code());
        }
        System.out.println(reportExeResponseStr);

        Map<String, Object> reportExeResponseData = convertJsonToMap(reportExeResponseStr);

        String requestId = (String) reportExeResponseData.get("requestId");
        List<Map<String, String>> exports = (List<Map<String, String>>) reportExeResponseData.get("exports");
        String reportExecutionStatus = exports.get(0).get("status");
        String exportId = exports.get(0).get("id");

        // GET THE REPORT NOW

        JASPER_REPORT_RESOURCE_URL_REST = JASPER_REPORT_RESOURCE_URL_REST + requestId + "/";
        Request jasperReportRequest = new Request.Builder()
                .url(JASPER_REPORT_RESOURCE_URL_REST)
                .addHeader("content-type", "application/xml")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", authHeader)
                .addHeader("Cookie", String.valueOf(cookies))
                .build();

        REPORT_OUTPUT_RESOURCE_URL = JASPER_REPORT_RESOURCE_URL_REST + "exports/" + exportId + "/outputResource";

        checkStatusAndDownloadReport(okHttpClient, jasperReportRequest, String.valueOf(cookies), authHeader);

    }
    public static void apiRun(Map<String, String>  parameters, String type){
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .build();
        if(type.equals("rest")){
            // jasperEmbeddingWithRESTAPI(okHttpClient,parameters);
        }else{
            try {
                jasperEmbeddingWithHttpAPI(okHttpClient,parameters);
            } catch (URISyntaxException | IOException e) {
               System.out.println("The error obtained is "+e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws URISyntaxException, IOException {

        Map<String, String> parameters= new HashMap<>();
        parameters.put("byEmail","mabera@gmail.com");

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .build();

        // jasperEmbeddingWithRESTAPI(okHttpClient,parameters);
        // Use okHttpClient here
        jasperEmbeddingWithHttpAPI(okHttpClient,parameters);

    }
}
