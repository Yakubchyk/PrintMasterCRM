package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Client;
import com.springboot.printmastercrm.entity.User;
import com.springboot.printmastercrm.repository.ClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public List<Client> getClientsByUser(User user) {

            return clientRepository.findByManagerId(user.getId());

    }

    public Client addClient(Client client, User manager) {
        return clientRepository.save(client);
    }

    public Client updateClient(Long id, Client client, User manager) {

        return clientRepository.save(client);
    }

    public void deleteClient(Long id, Client client) {

        clientRepository.delete(client);
    }
}