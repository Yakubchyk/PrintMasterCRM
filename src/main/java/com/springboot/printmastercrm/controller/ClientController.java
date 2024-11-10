package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Client;
import com.springboot.printmastercrm.entity.User;
import com.springboot.printmastercrm.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public List<Client> getClients(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return clientService.getClientsByUser(user);
    }

    @PostMapping
    public Client addClient(@RequestBody Client client, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return clientService.addClient(client, user);
    }

    @PutMapping("/{id}")
    public Client updateClient(@PathVariable Long id, @RequestBody Client client, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return clientService.updateClient(id, client, user);
    }

    @DeleteMapping("/{id}")
    public void deleteClient(@PathVariable Long id, Authentication auth) {
        User user = (User) auth.getPrincipal();
        clientService.deleteClient(id, user);
    }
}