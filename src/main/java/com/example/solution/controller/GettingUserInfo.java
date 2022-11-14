package com.example.solution.controller;

import com.example.solution.model.RequestUserInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/getUserInfo")
@Api(description = "Controller")
public class GettingUserInfo {
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiOperation("test")
    public ResponseEntity getUserInfo(@RequestBody String json,
                                      @RequestHeader("vk_service_token") String token)
    {
        try {
            Gson g = new Gson();
            RequestUserInfo info = g.fromJson(json, RequestUserInfo.class);
            if (info.getUser_id() == null || token == null) {
                return new ResponseEntity("invalid data", HttpStatus.OK);
            }

            String getURL1 =
                    "https://api.vk.com/method/users.get?" +
                    "user_ids=" + info.getUser_id().toString() +
                    "&fields=nickname" +
                    "&access_token=" + token  +
                    "&v=5.131";

            URL url1 = new URL(getURL1);
            HttpURLConnection request1 = (HttpURLConnection) url1.openConnection();
            request1.setRequestMethod("GET");

            String getURL2 =
                    "https://api.vk.com/method/groups.isMember?" +
                    "&group_id=" + info.getGroup_id().toString() +
                    "&user_id=" + info.getUser_id().toString() +
                    "&access_token=" + token +
                    "&extended=1" +
                    "&v=5.131";
            URL url2 = new URL(getURL2);
            HttpURLConnection request2 = (HttpURLConnection) url2.openConnection();

            String response1 = new BufferedReader(
                    new InputStreamReader(request1.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));

            String response2 = new BufferedReader(
                    new InputStreamReader(request2.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));

            JsonObject jsonObject1 = g.fromJson(response1, JsonObject.class);
            JsonObject jsonObject2 = g.fromJson(response2, JsonObject.class);

            JsonArray userInfoResponse;
            JsonObject isMemberResponse;

            if ((userInfoResponse = jsonObject1.getAsJsonArray("response")) == null) {
                return new ResponseEntity(
                        jsonObject1
                                .getAsJsonObject("error")
                                .get("error_msg")
                                .toString()
                        , HttpStatus.BAD_REQUEST);
            }
            if (userInfoResponse.isEmpty()) {
                return new ResponseEntity(
                        "user = {" + info.getUser_id().toString() + "} not found",
                        HttpStatus.NOT_FOUND
                );
            }
            JsonObject userInfo = userInfoResponse.get(0).getAsJsonObject();

            if ((isMemberResponse = jsonObject2.getAsJsonObject("response")) == null) {
                return new ResponseEntity(
                        jsonObject2
                                .getAsJsonObject("error")
                                .get("error_msg")
                                .toString()
                        , HttpStatus.BAD_REQUEST);
            }

            if (userInfo.get("deactivated") != null) {
                return new ResponseEntity("user profile has deactivated", HttpStatus.BAD_REQUEST);
            }

            JsonObject result = new JsonObject();
            result.add("last_name", userInfo.get("last_name"));
            result.add("first_name", userInfo.get("first_name"));
            result.add("middle_name", userInfo.get("nickname"));
            result.addProperty("member", isMemberResponse.get("member").toString().equals("1"));
            return new ResponseEntity(g.toJson(result), HttpStatus.valueOf(request1.getResponseCode()));
        }
        catch (Exception e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
