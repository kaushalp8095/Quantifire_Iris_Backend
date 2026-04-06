package com.project.client.dto;

import java.util.List;
import java.util.Map;
import com.project.client.models.clientDashboardModel;

public class dashboardResponse {

	public int totalCampaigns;
    public int totalLeads;
    public int totalCustomers;
    public double totalConversionRate;
    
    public List<clientDashboardModel> campaigns ;
    public Map<String, Integer> topLocations;
}
