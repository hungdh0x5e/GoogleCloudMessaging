<?php

/**
 * Registering a user device
 * Store reg id in users table
 */
$body = file_get_contents('php://input');
$postvars = json_decode($body, true);

if (isset($postvars["name"]) && isset($postvars["email"]) && isset($postvars["regId"])) {
    $name = $postvars["name"];
    $email = $postvars["email"];
    $gcm_regid = $postvars["regId"]; // GCM Registration ID
    // Store user details in db
    include_once './db_functions.php';
    include_once './GCM.php';

    $db = new DB_Functions();
    // $gcm = new GCM();
    if($db->isTokenExisted($gcm_regid)){
        $response["status"] = "false";
        $response["error"] = "Registration token is existed!";
        echo json_encode($response);
    }else{
        $res = $db->storeUser($name, $email, $gcm_regid);
            if($res){
                $response["status"] = "ok";
                $response["message"] = "Register success!";
                echo json_encode($response);
            }else{
                $response["status"] = "false";
                $response["error"] = "Registration token is existed!";
                echo json_encode($response);
            }
    }
} else {
    $response["status"] = "false";
    $response["error"] = "Param include: name, email, token";
    echo json_encode($response);
}
?>