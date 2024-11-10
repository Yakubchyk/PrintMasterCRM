package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Client;
import com.springboot.printmastercrm.entity.User;
import com.springboot.printmastercrm.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public List<Client> getClientsByUser(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return clientRepository.findAll();
        } else {
            return clientRepository.findByManagerId(user.getId());
        }
    }

    public Client addClient(Client client, User manager) {
        return clientRepository.save(client);
    }

    public Client updateClient(Long id, Client client, User manager) {

        return clientRepository.save(client);
    }

    public void deleteClient(Long id, User user) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        if (user.getRole() != User.Role.ADMIN && !client.getManager().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to client");
        }
        clientRepository.delete(client);
    }
}