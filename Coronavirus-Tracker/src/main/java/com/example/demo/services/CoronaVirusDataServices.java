package com.example.demo.services;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.model.LocationStates;

@Service
public class CoronaVirusDataServices {

    private List<LocationStates> allstates = new ArrayList<>();
    private int totalNewDeaths = 0;
    private String lastUpdated = "";

    public List<LocationStates> getAllstates() {
        return allstates;
    }

    public int getTotalNewDeaths() {
        return totalNewDeaths;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    private static final String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_deaths_global.csv";

    @PostConstruct
    @Scheduled(cron = "0 0 1 * * *")
    public void fetchVirusData() {
        List<LocationStates> newStates = new ArrayList<>();
        totalNewDeaths = 0;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(VIRUS_DATA_URL)).build();
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            StringReader csvBodyReader = new StringReader(httpResponse.body());
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);

            for (CSVRecord record : records) {
                LocationStates locationState = new LocationStates();
                locationState.setState(record.get("Province/State"));
                locationState.setCountry(record.get("Country/Region"));
                int latestCase = Integer.parseInt(record.get(record.size() - 1));
                int prevCase = Integer.parseInt(record.get(record.size() - 2));
                locationState.setLatestTotalDeaths(latestCase);
                locationState.setDifferFromPrevay(latestCase - prevCase);
                totalNewDeaths += (latestCase - prevCase);

                newStates.add(locationState);
            }

            this.allstates = newStates;
            this.lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
