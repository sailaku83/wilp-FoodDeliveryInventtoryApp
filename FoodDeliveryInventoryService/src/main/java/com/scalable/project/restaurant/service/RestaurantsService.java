package com.scalable.project.restaurant.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.scalable.project.FoodDeliveryInventoryApp;
import com.scalable.project.restaurant.entity.Restaurants;
import com.scalable.project.restaurant.repository.RestaurantRepository;

@Service
public class RestaurantsService {

	@Autowired
	private RestaurantRepository restuarantRepository;

	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${authTokenURL}")
	private String authTokenURL;

	@Value("${authURL}")
	private String authURL;

	@Value("${tokenUserName}")
	private String getTokenUserName;

	@Value("${tokenpassword}")
	private String getTokenPassword;

	private static final Logger log = LoggerFactory.getLogger(RestaurantsService.class);

	public List<Restaurants> findAll() {
		return restuarantRepository.findAll();
	}

	public Optional<Restaurants> findById(Long id) {
		return restuarantRepository.findById(id);
	}

	public Restaurants save(String userName, Restaurants Restaurants) throws Exception {
		if (!isAuthorized(authURL, userName, "Add Menu")) {
			throw new Exception("Not Authorised");
		}
		return restuarantRepository.save(Restaurants);
	}

	public void deleteById(Long id) {
		/*
		 * if(!isAuthorized(authURL, userName, "Delete Menu")) { throw new
		 * Exception("Not Authorised"); }
		 */
		restuarantRepository.deleteById(id);
	}

	public List<Restaurants> findByName(String name) {
		return restuarantRepository.findByName(name);
	}

	/**
	 * 
	 * @param url
	 * @param userName
	 * @param password
	 * @return
	 */
	public String getToken(String url, String userName, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> jsonDataMap = new HashMap<>();
		jsonDataMap.put("username", userName);
		jsonDataMap.put("password", password);

		// Convert map to JSON string
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonData = null;
		try {
			jsonData = objectMapper.writeValueAsString(jsonDataMap);
		} catch (JsonProcessingException e) {
			log.error("Error in authorize call" + e.getMessage());
			return null;
		}

		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonData, headers);
		ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
		// You can handle the response here if needed
		String jsonString = responseEntity.getBody();
		log.info("Response: " + jsonString);
		try {
			// Parse the JSON string
			JsonNode jsonNode = objectMapper.readTree(jsonString);

			// Get the value of the "token" field
			String token = jsonNode.get("token").asText();
			return token;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param url
	 * @param userName
	 * @param operation
	 * @return
	 */
	public boolean isAuthorized(String url, String userName,String operation) {
		String token = getToken(authTokenURL, getTokenUserName, getTokenPassword);
		if(token == null) {
			log.error("Error in gettting token");
			return false;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", "Token " + token);
		Map<String, Object> jsonDataMap = new HashMap<>();
		jsonDataMap.put("username", userName);
		jsonDataMap.put("action", operation);

		// Convert map to JSON string
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonData = null;
		try {
			jsonData = objectMapper.writeValueAsString(jsonDataMap);
		} catch (JsonProcessingException e) {
			log.error("Error in authorize call" + e.getMessage());
			return false;
		}

		HttpEntity<Object> requestEntity = new HttpEntity<>(jsonData, headers);
		ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
		// You can handle the response here if needed
		String jsonString = responseEntity.getBody();
		log.info("Response: " + jsonString);
		try {
			// Parse the JSON string
			JsonNode jsonNode = objectMapper.readTree(jsonString);

			// Get the value of the "token" field
			String isPermitted = jsonNode.get("message").asText();
			if(isPermitted != null && isPermitted.equals("Permitted")) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

}
