package org.sust_unknowns.javafest.userservices.controller;

import org.sust_unknowns.javafest.userservices.model.Agent;
import org.sust_unknowns.javafest.userservices.model.VerifyUser;

import org.sust_unknowns.javafest.userservices.service.MailService;

import org.sust_unknowns.javafest.userservices.utils.codeGenarator;
import org.sust_unknowns.javafest.userservices.utils.JwtUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.sust_unknowns.javafest.userservices.service.AgentService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestController
@RequestMapping("/api/agents")
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final MailService mailService;
    private final codeGenarator codeGenarator;
    private final JwtUtils jwtUtils;

    public AgentController(MailService mailService, AgentService agentService) {
        this.mailService = mailService;
        this.codeGenarator = new codeGenarator();
        this.jwtUtils = new JwtUtils();
        this.agentService = agentService;
    }

    @PostMapping("/resend")
    public ResponseEntity<Boolean> resendCode(@RequestParam String id) {

        if (id == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Agent agent = agentService.getAgentById(id);
        mailService.sendMail(agent.getEmail(), "Agribazaar - Verification Code (Resend)",
                formatTheMessage(agent.getCode(), agent.getName()));
        return ResponseEntity.ok(true);
    }

    @PostMapping("/register")
    public ResponseEntity<String> createAgent(@RequestBody Agent agent) {
        System.out.println(agent.getEmail());
        if (agent.getEmail() == null || agent.getName() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please fill all the fields");
        }
        if (agentService.checkAgent(agent.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Agent already exist");
        }
        Agent agent1 = new Agent();
        agent1.setName(agent.getName());
        agent1.setEmail(agent.getEmail());
        agent1.setPassword(codeGenarator.hashPassword(agent.getPassword()));
        agent1.setCode(codeGenarator.generateCode());
        agent1.setVerified(false);
        agentService.saveAgent(agent1);
        mailService.sendMail(agent1.getEmail(), "Verification Code",
                formatTheMessage(agent1.getCode(), agent1.getName()));
        return ResponseEntity.status(HttpStatus.OK).body(agent1.getId());
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyAgent(@RequestBody VerifyUser agent) {
        if (agent.getId() == null || agent.getCode() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please fill all the fields");
        }
        Agent agent1 = agentService.getAgentById(agent.getId());
        if (agent1 == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Agent not found");
        }
        if (agent1.getCode().equals(agent.getCode())) {
            agent1.setVerified(true);
            agentService.updateAgent(agent1);
            return ResponseEntity.status(HttpStatus.OK).body("Agent verified successfully");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid code");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginAgent(@RequestBody Agent agent) {
        if (agent.getEmail() == null || agent.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please fill all the fields");
        }
        Agent agent1 = agentService.getAgentByEmail(agent.getEmail());
        if (agent1 == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Agent not found");
        }
        if (agent1.isVerified()) {
            if (codeGenarator.checkPassword(agent.getPassword(), agent1.getPassword())) {
                String token = jwtUtils.generateToken(agent1.getId());
                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", token);
                return ResponseEntity.status(HttpStatus.OK).headers(headers).body(token);
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid password");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Agent not verified");
    }

    @GetMapping("/getagent")
    public ResponseEntity<Agent> getAgent(@RequestHeader("Authorization") String token) {
        String id = jwtUtils.validateToken(token);

        if (id == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Invalid token
        }

        Agent agent = agentService.getAgentById(id);

        if (agent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // User not found
        }

        return ResponseEntity.ok(agent);
    }

    private String formatTheMessage(String code, String name) {

        return "<html>" +
                "<head></head>" +
                "<body>" +
                "<div class='container'>" +

                "Dear " + name + "," +
                "<h1>Hi, Welcome to <span style='color: #425119; font-family: Caveat, cursive;'>agribazaar</span></h1>"
                +
                "<p>Thank you for choosing to become an agribazaar agent.</p>" +
                "<p>Your verification code is: <b>" + code + "</b></p>" +
                "<p>Visit our website at <a href='https://agribazaar.vercel.app'>www.agribazaar.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";

    }

    private String formatTheMessageadmin(Agent agent) {
        return "<html>" +
                "<head></head>" +
                "<body>" +
                "<div class='container'>" +

                "<h1>Hi, Welcome to <span style='color: #425119; font-family: Caveat, cursive;'>agribazaar</span></h1>"
                +
                "<p>New Agent registration</p>" +
                "<p>Name: " + agent.getName() + "</p>" +
                "<p>Email: " + agent.getEmail() + "</p>" +
                "<p>Phone: " + agent.getPhone() + "</p>" +
                "<p>Address: " + agent.getAddress() + "</p>" +
                "<p>Nid Number: " + agent.getNidNumber() + "</p>" +
                "<p>Thank you for choosing us.</p>" +
                "<p>Visit our website at <a href='https://agribazaar.vercel.app'>www.agribazaar.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // send agent update copy to the agent
    private String formatTheMessage(Agent agent) {
        return "<html>" +
                "<head></head>" +
                "<body>" +
                "<div class='container'>" +
                // "<img src='your_logo.png' alt='KIWI Logo' class='logo'>" +
                "<h1>Hi, Welcome to <span style='color: #425119; font-family: Caveat, cursive;'>agribazaar</span></h1>"
                +
                "<p>Your profile has been updated successfully.</p>" +
                "<p>Name: " + agent.getName() + "</p>" +
                "<p>Email: " + agent.getEmail() + "</p>" +
                "<p>Phone: " + agent.getPhone() + "</p>" +
                "<p>Address: " + agent.getAddress() + "</p>" +
                "<p>Nid Number: " + agent.getNidNumber() + "</p>" +
                "<p>Thank you for choosing us.</p>" +
                "<p>Visit our website at <a href='https://agribazaar.vercel.app'>www.agribazaar.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateAgentProfileById(
            @RequestBody Agent updateRequest,
            @RequestParam("email") String email) {

        try {
            if (email == null || updateRequest == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email or update request is missing.");
            }
            log.info("Received email for update: {}", email);

            Agent agent = agentService.getAgentByEmail(email);
            if (agent == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }
            log.info("Updating user with email: {}", agent.getEmail());

            agent.setName(updateRequest.getName());
            log.info("Updated name: {}", updateRequest.getName());

            // Hash the password only if it was changed
            if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
                agent.setPassword(codeGenarator.hashPassword(updateRequest.getPassword()));
            }
            agent.setAddress(updateRequest.getAddress());
            log.info("Updated address: {}", updateRequest.getAddress());
            agent.setPhone(updateRequest.getPhone());
            agent.setGender(updateRequest.getGender());
            agent.setNidNumber(updateRequest.getNidNumber());
            agent.setNidImage(updateRequest.getNidImage());
            agent.setSignatureImage(updateRequest.getSignatureImage());
            agent.setAvatar(updateRequest.getAvatar());
            agentService.updateAgent(agent);
            mailService.sendMail(agent.getEmail(), "Agribazaar - Profile Updated",
                    formatTheMessage(agent));
            mailService.sendMail("nobelbadhon61@gmail.com", "Agribazaar - New Agent Registration",
                    formatTheMessageadmin(agent));

            mailService.sendMail("abdullahalmahadiapurbo@gmail.com", "Agribazaar - New Agent Registration",
                    formatTheMessageadmin(agent));
            return ResponseEntity.ok(agent);

        } catch (MaxUploadSizeExceededException e) {
            log.error("File upload size exceeded", e);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File upload size exceeded.");
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating the user.");
        }
    }

    // Send OTP to farmer for verification
    @PutMapping("/sendotp")
    public ResponseEntity<?> sendOTP(@RequestParam String email, @RequestParam String id) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please provide email");
        }
        Agent agent = agentService.getAgentById(id);
        if (agent == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Agent not found");
        }
        String otp = codeGenarator.generateCode();
        agent.setOTP(otp);
        agentService.updateAgent(agent);
        mailService.sendMail(email, "Agribazaar - Verification Code",
                formatTheMessage(otp, agent.getName(), agent.getId()));
        return ResponseEntity.status(HttpStatus.OK).body("OTP sent successfully");
    }

    // Verify OTP
    @PostMapping("/verifyotp")
    public ResponseEntity<?> verifyOTP(@RequestParam String id, @RequestParam String otp) {
        if (id == null || otp == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please fill all the fields");
        }
        Agent agent = agentService.getAgentById(id);
        if (agent == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Agent not found");
        }
        if (agent.getOTP().equals(otp)) {
            return ResponseEntity.status(HttpStatus.OK).body("OTP verified successfully");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP");
    }

    // get all agent
    @GetMapping("/all")
    public ResponseEntity<?> getAllAgent() {
        return ResponseEntity.ok(agentService.getAllAgent());
    }

    // Send OTP to farmer for verification
    private String formatTheMessage(String code, String agentName, String id) {
        return "<html>" +
                "<head></head>" +
                "<body>" +
                "<div class='container'>" +
                "<h1>Welcome to <span style='color: #425119; font-family: Caveat, cursive;'>Agribazaar</span></h1>" +
                "<p>Dear User,</p>" +
                "<p>Agent <b>" + agentName + "</b>, with ID <b>" + id
                + "</b>, has requested access to your Agribazaar account.</p>" +
                "<p>Please use the following verification code to proceed:</p>" +
                "<p style='font-size: 18px;'><b>" + code + "</b></p>" +
                "<p>If you did not initiate this request, please contact our support team immediately.</p>" +
                "<p>For more information, visit our website at <a href='https://agribazaar.vercel.app'>www.agribazaar.com</a></p>"
                +
                "<p>Thank you,</p>" +
                "<p>The Agribazaar Team</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

}
