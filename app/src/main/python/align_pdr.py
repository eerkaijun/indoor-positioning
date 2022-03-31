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

def align_pdr(sensor, sex, height):

    sensor = int(sensor)
    sex = int(sex)
    height = int(height)

    #Read input files
    #inputfile = 'Sensor_20220321_190524_8170278612129910134.csv'
    #filename = join(dirname(__file__), inputfile)
    #inputfile_reader = pd.read_csv(filename)
    #inputfile = str(filename)

    inputfile_1 = 'Sensor_20220324_164917_6405095473183875478.csv'
    inputfile_2 = 'Sensor_20220324_165108_1498070707813036265.csv'

    filename_1 = join(dirname(__file__), inputfile_1)
    inputfile_reader_1 = pd.read_csv(filename_1)
    filename_2 = join(dirname(__file__), inputfile_2)
    inputfile_reader_2 = pd.read_csv(filename_2) # Make each row a list with one string-type element
    inputfile_reader = pd.concat([inputfile_reader_1, inputfile_reader_2.iloc[1:]])
    inputfile_reader = inputfile_reader.reset_index()
    inputfile_reader = inputfile_reader.drop(columns = 'index')

    #Declare variables
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
    #avg_angle = 0
    step_counter  = 0
    total_dist = 0
    prev_angle = 0
    total_dist_arr = []
    step_arr = []
    bias = 12
    sensor_vals = []

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

    #Append for starting position
    #x_vals.append(0)
    #y_vals.append(0)
    prev_x = 0
    prev_y = 0


    #Convert vector acceleration to scalar
    acc_arr = []
    scalar_acc = 0

    for i in range(len(inputfile_reader)):
        scalar_acc = np.sqrt((inputfile_reader.iloc[i, 4]**2) + (inputfile_reader.iloc[i, 5]**2) +
                               (inputfile_reader.iloc[i, 4]**2))
        acc_arr.append(scalar_acc)

    #Remove gravity
    acc_new = []
    ave_acc = np.mean(acc_arr)
    for i in range(len(inputfile_reader)):
        acc_pavg = acc_arr[i] * (1 - alpha) + ave_acc * alpha
        acc_new.append(acc_arr[i] - acc_pavg)


    #Calculate and plot trajectory
    #-------------------------------------------------------------------

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
        #if (np.sign(inputfile_reader.iloc[i,10]) != np.sign(inputfile_reader.iloc[(i + 1), 10])):
        #print(i, np.sign(inputfile_reader.iloc[i,10]))
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
    angle_offset = np.mean(d_m[:ratio])

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

    Z, upper, lower = prepare_z(x_vals, y_vals, sensor_vals)

    #Plot trajectory
    image_name = 'map.png'
    image_file = join(dirname(__file__), image_name)
    img = mpimg.imread(image_file)
    fig = plt.figure()
    a1 = fig.add_subplot(1,1,1)
    a1.imshow(img, extent=[-90, 60, -120, 40])
    #a1.title(measurement[sensor])
    line = a1.contour(x_vals, y_vals, Z, levels = np.linspace(lower,upper, 20), linewidths = 5, cmap='Blues', interpolation='none')
    fig.colorbar(line, ax = a1)
    x_ticks = np.around(np.linspace(55.92205, 55.92340, len(Z)), decimals = 6)
    y_ticks = np.around(np.linspace(-3.17318, -3.17067, len(Z)), decimals = 6)
    a1.grid()

    #Change meters to global coordinates
    locs_x = x_vals
    labels_x = x_ticks
    locs_y = y_vals
    labels_y = y_ticks
    #a1.set_xticks(locs_x)
    a1.set_xticklabels(labels_x, rotation=25)
    #a1.set_yticks(locs_y)
    a1.set_yticklabels(labels_y, rotation=0)
    #a1.set_xlabel('Horizontal displacement (meters)')
    #a1.set_ylabel('Vertical displacement (meters)')

    #Use this canvas to convert image to numpy array
    fig.canvas.draw()
    img = np.fromstring(fig.canvas.tostring_rgb(), dtype = np.uint8, sep = '')
    img = img.reshape(fig.canvas.get_width_height()[::-1] + (3,))
    #img = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)

    #Convert to PIL image and then to byte string to return it in the java code
    pil_im = Image.fromarray(img)
    buff = io.BytesIO()
    pil_im.save(buff, format = 'PNG')
    img_str = base64.b64encode(buff.getvalue())
    out = "" + str(img_str, 'utf-8')
    
    return out

    













