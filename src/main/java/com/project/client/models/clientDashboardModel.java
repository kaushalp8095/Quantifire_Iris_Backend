package com.project.client.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class clientDashboardModel {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String clientEmail; 
    private String name;
    private String location;
    private String status;
    private int leads;
    private int customers;
	public clientDashboardModel() {
		super();
		// TODO Auto-generated constructor stub
	}
	public clientDashboardModel(Long id, String clientEmail, String name, String location, String status, int leads,
			int customers) {
		super();
		this.id = id;
		this.clientEmail = clientEmail;
		this.name = name;
		this.location = location;
		this.status = status;
		this.leads = leads;
		this.customers = customers;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getClientEmail() {
		return clientEmail;
	}
	public void setClientEmail(String clientEmail) {
		this.clientEmail = clientEmail;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getLeads() {
		return leads;
	}
	public void setLeads(int leads) {
		this.leads = leads;
	}
	public int getCustomers() {
		return customers;
	}
	public void setCustomers(int customers) {
		this.customers = customers;
	}
    
    
}
