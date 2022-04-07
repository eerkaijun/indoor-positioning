#Created by Petros Koutsouvelis in 03/2022

from os.path import dirname, join
from PIL import Image
import matplotlib
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np
import io
import base64

def align_pdr(sensor, sex, height, start_pos):
    #Read function inputs:
    #----------------------------------------------------------------------------
    sensor = int(sensor)
    sex = int(sex)
    height = int(height)

    #Coordinate values of start-end points in all corridors
    lat_corners = [55.9230914, 55.9230448, 55.9225607, 55.9224297, 55.9224406,
                   55.9223084, 55.9227005, 55.922693, 55.9227185, 55.9226569]
    long_corners = [-3.17163, -3.1717641, -3.1712769, -3.1716507, -3.1717245,
                    -3.172093, -3.1725526, -3.1725745, -3.1726026, -3.1727689]
    start_pos = [long_corners[start_pos], lat_corners[start_pos]]


    #Read input files:
    #-----------------------------------------------------------------------------
    inputfile_1 = 'Sensor_20220324_164917_6405095473183875478.csv'
    inputfile_2 = 'Sensor_20220324_165108_1498070707813036265.csv'

    filename_1 = join(dirname(__file__), inputfile_1)
    inputfile_reader_1 = pd.read_csv(filename_1)
    filename_2 = join(dirname(__file__), inputfile_2)
    inputfile_reader_2 = pd.read_csv(filename_2) # Make each row a list with one string-type element
    inputfile_reader = pd.concat([inputfile_reader_1, inputfile_reader_2.iloc[1:]])
    inputfile_reader = inputfile_reader.reset_index()
    inputfile_reader = inputfile_reader.drop(columns = 'index')

    #Declare variables:
    #-----------------------------------------------------------------------------
    x_vals = []
    y_vals = []
    alpha = 0.9 #filter parameter
    threshold = -0.085 #step detection threshold
    m = 5 #smoothing factor
    min_ang = 10
    max_ang = 70
    step_detect = 0
    new_x = 0
    new_y = 0
    step_counter  = 0
    total_dist = 0
    prev_angle = 0
    total_dist_arr = []
    step_arr = []
    sensor_vals = []
    prev_x = 0
    prev_y = 0

    #Row indices for every 0.5 seconds (? to be changed), depending on the sampling rate
    #Assume sampling rate is 4 Hz
    sampling_rate = 4.8
    desired_rate = 2
    ratio = int(sampling_rate/desired_rate) #data to read every iteration


    #Set step length based on inputs
    if (sex):
        k = 0.415
    else:
        k = 0.413
    l = (k * height)/100 #to meters

    #Data pre-processing:
    #-----------------------------------------------------------------------------
    #Convert vector acceleration to scalar
    acc_arr = []

    for i in range(len(inputfile_reader)):
        scalar_acc = np.sqrt((inputfile_reader.iloc[i, 4]**2) + (inputfile_reader.iloc[i, 5]**2) +
                               (inputfile_reader.iloc[i, 4]**2))
        acc_arr.append(scalar_acc)

    #High-pass filter to remove gravity after isolating it with a low-pass filter
    acc_new = []
    ave_acc = np.mean(acc_arr)
    for i in range(len(inputfile_reader)):
        acc_pavg = acc_arr[i] * (1 - alpha) + ave_acc * alpha
        acc_new.append(acc_arr[i] - acc_pavg)


    #Perform smoothing as in paper "Fusion of WiFi, Smartphone Sensors
    #and Landmarks Using the Kalman Filter for Indoor Localization"

    a_m = [] #acceleration after smoothing
    for i in range(len(inputfile_reader) - m):
        sum_m = 0
        for j in range(m):
            sum_m = sum_m + acc_new[i + j]
        avg = sum_m/m
        a_m.append(avg)

    #correct angles, optimize code
    new_deg = []
    for i in range(len(inputfile_reader)):
        if (inputfile_reader.iloc[i,10] > 50):
            new_deg.append(inputfile_reader.iloc[i,10] - 360)
        else:
            new_deg.append(inputfile_reader.iloc[i,10])

    #Smoothing for degrees
    #degrees_z = inputfile_reader.iloc[:, 10].values #angles from rotation matrix
    degrees_z = new_deg
    d_m = [] #angle after smoothing
    for i in range(len(inputfile_reader) - m):
        sum_m = 0
        for j in range(m):
            sum_m = sum_m + degrees_z[i + j]
        avg = sum_m/m
        d_m.append(avg)

    #Calculate and plot trajectory
    #-------------------------------------------------------------------
    #Compute new coordinates
    for i in range(0, (len(a_m) - ratio + 1), ratio):
        avg_angle = np.mean(d_m[i : (i + ratio)]) 
        avg_val = np.mean(inputfile_reader.iloc[i : (i + ratio), sensor])
        for j in a_m[i : (i + ratio)]:
            if (j > threshold):
                step_detect = 1
        if (step_detect):
            if (abs(prev_angle - avg_angle) < min_ang): #Walk straight
                new_x = prev_x + l * np.sin(np.deg2rad(prev_angle)) #np.deg2rad(avg_angle)
                new_y = prev_y + l * np.cos(np.deg2rad(prev_angle)) #np.deg2rad(avg_angle)
            elif (abs(prev_angle - avg_angle) > min_ang) and (abs(prev_angle - avg_angle) < max_ang):
                new_x = prev_x + l * np.sin(np.deg2rad(avg_angle)) #np.deg2rad(avg_angle)
                new_y = prev_y + l * np.cos(np.deg2rad(avg_angle)) #np.deg2rad(avg_angle)
                prev_angle = avg_angle
            else:
                if (prev_angle - avg_angle) < 0:
                    avg_angle = prev_angle + 90
                    new_x = prev_x + l * np.sin(np.deg2rad(avg_angle)) #np.deg2rad(avg_angle)
                    new_y = prev_y + l * np.cos(np.deg2rad(avg_angle)) #np.deg2rad(avg_angle)
                    prev_angle = avg_angle
                else:
                    avg_angle = prev_angle - 90
                    new_x = prev_x + l * np.sin(np.deg2rad(avg_angle)) #np.deg2rad(avg_angle)
                    new_y = prev_y + l * np.cos(np.deg2rad(avg_angle)) #np.deg2rad(avg_angle)
                    prev_angle = avg_angle
            step_counter = step_counter + 1
            total_dist = total_dist + l
            prev_x = new_x
            prev_y = new_y
        x_vals.append(new_x)
        y_vals.append(new_y)
        sensor_vals.append(avg_val)
        step_arr.append(step_detect)
        total_dist_arr.append(total_dist)
        step_detect = 0

    #Figure titles
    measurement = ['Magnetometer measurement (uT): X-axis',
                   'Magnetometer measurement (uT): Y-axis',
                   'Magnetometer measurement (uT): Z-axis',
                   'Magnetic field strength measurement (V/m)',
                   'Accelerometer measurement (m/s2): X-axis',
                   'Accelerometer measurement (m/s2): Y-axis',
                   'Accelerometer measurement (m/s2): Z-axis',
                   'Gyroscope measurement (deg/s): X-axis',
                   'Gyroscope measurement (deg/s): Y-axis',
                   'Gyroscope measurement (deg/s): Z-axis',
                   'Degrees measurement (deg)']

    #function to convert meters to global coordinates
    def metres_to_deg(x_vals, y_vals, start_pos):
        start_long = start_pos[0]
        start_lat = start_pos[1]
        long_val = start_long
        lat_val = start_lat
        long_vals = []
        lat_vals = []
        latdeg_per_m = 1/111340.33010639016
        longdeg_per_m = 1/62521.56943020176
        for i in range(len(x_vals)-1):
            long_val = long_val + longdeg_per_m * (x_vals[i+1] - x_vals[i])
            lat_val = lat_val + latdeg_per_m * (y_vals[i+1] - y_vals[i])
            long_vals.append(long_val)
            lat_vals.append(lat_val)
        return long_vals, lat_vals

    #Call function
    long_vals, lat_vals = metres_to_deg(x_vals, y_vals, start_pos)

    #Define contour
    def prepare_z(X, Y, I):
        Z = []
        for i in range(len(X)):
            Z_1 = []
            for j in range(len(X)): #X, Y, I of equal size
                Z_1.append(-1000)
            Z_1[i] = I[i]
            Z.append(Z_1)
        upper = max(I)
        lower = min(I)
        return Z, upper, lower

    Z, upper, lower = prepare_z(long_vals, lat_vals, sensor_vals)

    #Plot trajectory
    image_name = 'map.png'
    image_file = join(dirname(__file__), image_name)
    img = mpimg.imread(image_file)
    fig = plt.figure()
    a1 = fig.add_subplot(1,1,1)
    a1.imshow(img, extent=[-3.17310, -3.1708, 55.92205, 55.92340])
    line = a1.contour(long_vals, lat_vals, Z, levels = np.linspace(lower,upper, 20), linewidths = 5, cmap='Blues', interpolation='none')
    fig.colorbar(line, ax = a1)
    a1.grid()

    #Use this canvas to convert image to numpy array
    fig.canvas.draw()
    img = np.fromstring(fig.canvas.tostring_rgb(), dtype = np.uint8, sep = '')
    img = img.reshape(fig.canvas.get_width_height()[::-1] + (3,))

    #Convert to PIL image and then to byte string to return it in the java code
    pil_im = Image.fromarray(img)
    buff = io.BytesIO()
    pil_im.save(buff, format = 'PNG')
    img_str = base64.b64encode(buff.getvalue())
    out = "" + str(img_str, 'utf-8')
    
    return out

    













