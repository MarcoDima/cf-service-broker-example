package de.evoila.cf.broker.service.impl;

import de.evoila.cf.broker.bean.BackupConfiguration;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.service.BackupService;
import de.evoila.cf.broker.service.InstanceCredentialService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@ConditionalOnBean(BackupConfiguration.class)
public class BackupServiceImpl implements BackupService {
    private final RestTemplate rest;
    private final HttpHeaders headers;
    InstanceCredentialService credentialService;
    BackupConfiguration config;

    public BackupServiceImpl (BackupConfiguration config, InstanceCredentialService credentialService) {
        this.config = config;
        this.credentialService = credentialService;

        this.rest = new RestTemplate();
        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        headers.add("Authorization", encodeCredentials());
    }

    private String encodeCredentials () {
        String str = config.getUser()+":"+config.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(str.getBytes());
    }

    @Override
    public ResponseEntity<HashMap> backupNow (String serviceInstanceId, HashMap body) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        body.put("source", credentials);
        Object obj = body.get("destination");
        if(obj instanceof Map){
            ((Map) obj).put("type", "Swift");
        }

        RequestEntity e = new RequestEntity(body, headers,
                                            HttpMethod.POST,
                                            URI.create(config.getUri() + "/backup")
        );
        ResponseEntity response = rest.exchange(e, HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> restoreNow (String serviceInstanceId, HashMap body) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        body.put("destination", credentials);
        Object obj = body.get("source");
        if(obj instanceof Map){
            ((Map) obj).put("type", "Swift");
        }

        RequestEntity e  = new RequestEntity(body, headers, HttpMethod.POST,
                                             URI.create(config.getUri()+"/restore"));
        ResponseEntity response = rest.exchange(e,HashMap.class);
        return response;
    }

    @Override
    public ResponseEntity<HashMap> getJobs (String serviceInstanceId, Pageable pageable) {
        Map<String, String> uriParams = new HashMap<String, String>();
        uriParams.put("serviceInstanceId", serviceInstanceId);

        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<HashMap> response = rest
                .exchange(buildUri("/jobs/byInstance/{serviceInstanceId}", pageable).buildAndExpand(uriParams).toUri(),
                    HttpMethod.GET, entity, new ParameterizedTypeReference<HashMap>() {});

        return response;
    }

    private UriComponentsBuilder buildUri(String path, Pageable pageable) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(config.getUri() + path);

        builder.queryParam("page", pageable.getPageNumber());
        builder.queryParam("page_size", pageable.getPageSize());
        if (pageable.getSort() != null) {
            Iterator<Sort.Order> sortIterator = pageable.getSort().iterator();
            while (sortIterator.hasNext()) {
                Sort.Order order = sortIterator.next();
                builder.queryParam("sort", order.getProperty() + "," + order.getDirection().toString());
            }
        }

        return builder;
    }

    @Override
    public ResponseEntity<HashMap> getPlans (String serviceInstanceId, Pageable pageable) {
        Map<String, String> uriParams = new HashMap<String, String>();
        uriParams.put("serviceInstanceId", serviceInstanceId);


        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<HashMap> response = rest
                .exchange(buildUri("/plans/byInstance/{serviceInstanceId}", pageable).buildAndExpand(uriParams).toUri(),
                        HttpMethod.GET, entity, new ParameterizedTypeReference<HashMap>() {});

        return response;
    }

    @Override
    public ResponseEntity<HashMap> deleteJob (String serviceInstanceId, String jobid) {
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/jobs/" + jobid,
                                                HttpMethod.DELETE, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> postPlan (String serviceInstanceId, HashMap plan) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        plan.put("source", credentials);
        HttpEntity entity = new HttpEntity(plan, headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/plans",
                                                HttpMethod.POST, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> deletePlan (String serviceInstanceId, String planid) {
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/plans/" + planid,
                                                HttpMethod.DELETE, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> updatePlan (String serviceInstanceId, String planId, HashMap plan) throws ServiceInstanceDoesNotExistException {
        HashMap credentials = credentialService.getCredentialsForInstanceId(serviceInstanceId);
        plan.put("source", credentials);
        HttpEntity entity = new HttpEntity(plan, headers);
        ResponseEntity response = rest.exchange(config.getUri() + "/plans/" + planId,
                                                HttpMethod.PUT, entity, HashMap.class
        );
        return response;
    }

    @Override
    public ResponseEntity<HashMap> getJob (String serviceInstanceId, String jobid) {
        HttpEntity entity = new HttpEntity(headers);
        return rest.exchange(config.getUri()+"/jobs/"+jobid,HttpMethod.GET,entity,HashMap.class);
    }

    @Override
    public ResponseEntity<HashMap> getPlan (String serviceInstanceId, String planId) {
        HttpEntity entity = new HttpEntity(headers);
        return rest.exchange(config.getUri()+"/plans/"+planId,HttpMethod.GET,entity,HashMap.class);
    }

}
